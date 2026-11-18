package com.banco.pj.servico;

import com.banco.pj.dominio.*;
import com.banco.pj.infra.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

public class ServicoContaPJ implements OperacoesContaPJ {

    private final RepositorioContasEmMemoria repositorio;

    private final EnviadorEmail email = new EnviadorEmail();
    private final EnviadorSms sms = new EnviadorSms();
    private final List<String> auditoria = new ArrayList<>();

    private final AberturaContaValidador contaValidador;
    private final Auditoria auditoriaObject;

    public ServicoContaPJ(AberturaContaValidador contaValidador, RepositorioContasEmMemoria repositorio, Auditoria auditoriaObject) {
        this.contaValidador = contaValidador;
        this.repositorio = repositorio;
        this.auditoriaObject = auditoriaObject;
    }
    // ------------------------------------------------------------------
    // Abertura / encerramento
    // ------------------------------------------------------------------

    @Override
    public ContaPJ abrirConta(String cnpj, String razaoSocial, TipoEmpresa tipo, String emailContato, String telefone, BigDecimal depositoInicial) {
        contaValidador.validar(cnpj, razaoSocial, tipo, emailContato, depositoInicial);
        String cnpjLimpo = ValidadorCnpj.somenteDigitos(cnpj);

        BigDecimal deposito = depositoInicial.setScale(2, RoundingMode.HALF_UP);
        ContaPJ conta = AberturaContaPjCriacao.criar(tipo, cnpjLimpo, razaoSocial, emailContato, telefone, deposito);
        AberturaContaPjConfiguracao.configurarConta(tipo, conta);
        if (deposito.signum() > 0) {
            conta.registrarLancamento("DEPOSITO", "Deposito inicial", deposito);
        }

        repositorio.salvar(conta);
        auditoriaObject.registrar("ABERTURA_CONTA|" + conta.getCnpj() + "|" + conta.getTipo());
        email.enviar(emailContato, "Bem-vindo ao Banco PJ", "Conta aberta para " + conta.getRazaoSocial());
        return conta;
    }

    @Override
    public void encerrarConta(String cnpj) {
        ContaPJ conta = buscarAtiva(cnpj);
        if (conta.getSaldo().compareTo(BigDecimal.ZERO) != 0) {
            throw new OperacaoInvalidaException("Conta com saldo diferente de zero nao pode ser encerrada");
        }
        conta.setAtiva(false);
        auditoriaObject.registrar("ENCERRAMENTO_CONTA|" + conta.getCnpj());
        email.enviar(conta.getEmail(), "Conta encerrada", "Sua conta foi encerrada");
    }

    @Override
    public BigDecimal consultarSaldo(String cnpj) {
        return buscarAtiva(cnpj).getSaldo();
    }

    // ------------------------------------------------------------------
    // Movimentacoes
    // ------------------------------------------------------------------

    @Override
    public void depositar(String cnpj, BigDecimal valor) {
        ContaPJ conta = buscarAtiva(cnpj);
        BigDecimal v = valorPositivo(valor);

        conta.setSaldo(conta.getSaldo().add(v));
        conta.registrarLancamento("DEPOSITO", "Deposito em conta", v);
        auditoriaObject.registrar("DEPOSITO|" + conta.getCnpj() + "|" + v.toPlainString());

        if (v.compareTo(new BigDecimal("10000.00")) >= 0) {
            auditoriaObject.registrar("ALERTA_COAF|" + conta.getCnpj() + "|" + v.toPlainString());
            sms.enviar(conta.getTelefone(), "Deposito de R$ " + v.toPlainString() + " identificado");
        }
    }

    @Override
    public void sacar(String cnpj, BigDecimal valor) {
        ContaPJ conta = buscarAtiva(cnpj);
        BigDecimal v = valorPositivo(valor);

        BigDecimal tarifa = BigDecimal.ZERO;
        tarifa = DeterminarTarifa.executarTarifaOperacao(conta, tarifa);
        tarifa = normalizar(tarifa);

        BigDecimal total = v.add(tarifa);
        conta.validarSaldo(total);
        conta.debitar(v);
        conta.registrarLancamento("SAQUE", "Saque em conta", v);

        if (tarifa.signum() > 0) {
            conta.debitar(tarifa);
            conta.registrarLancamento("TARIFA", "Tarifa de saque", tarifa);
        }
        auditoriaObject.registrar("SAQUE|" + conta.getCnpj() + "|" + v.toPlainString());
    }

    @Override
    public void transferir(String cnpjOrigem, String cnpjDestino, BigDecimal valor, String tipoTransferencia) {
        BigDecimal valoredPositivo = validarTipoTransferencia(valor, tipoTransferencia);
        String tipo = tipoTransferencia.trim().toUpperCase();
        ContaPJ origem = buscarAtiva(cnpjOrigem);
        ContaPJ destino = buscarAtiva(cnpjDestino);

        if (origem.getCnpj().equals(destino.getCnpj())) {
            throw new OperacaoInvalidaException("Origem e destino iguais");
        }

        validarAntiFraude(valoredPositivo, origem);
        BigDecimal tarifa = obterTarifaTransferencia(tipoTransferencia, tipo, origem, valoredPositivo);
        tarifa = normalizar(tarifa);
        origem.validarSaldo(valoredPositivo.add(tarifa));
        efetivarTransferencia(origem, valoredPositivo, tipo, destino, tarifa);
        notificarTransferenciaLimiteAcima(valoredPositivo, origem);
    }

    private void notificarTransferenciaLimiteAcima(BigDecimal valoredPositivo, ContaPJ origem) {
        if (valoredPositivo.compareTo(new BigDecimal("50000.00")) >= 0) {
            sms.enviar(origem.getTelefone(), "Transferencia de R$ " + valoredPositivo.toPlainString() + " realizada");
        }
    }

    private void efetivarTransferencia(ContaPJ origem, BigDecimal valoredPositivo, String tipo, ContaPJ destino, BigDecimal tarifa) {
        origem.debitar(valoredPositivo);
        origem.registrarLancamento("TRANSFERENCIA_ENVIADA", tipo + " para " + destino.getCnpj(), valoredPositivo);
        if (tarifa.signum() > 0) {
            origem.debitar(tarifa);
            origem.registrarLancamento("TARIFA", "Tarifa de " + tipo, tarifa);
        }
        destino.creditar(valoredPositivo);
        destino.registrarLancamento("TRANSFERENCIA_RECEBIDA", tipo + " de " + origem.getCnpj(), valoredPositivo);
        auditoriaObject.registrar("TRANSFERENCIA|" + tipo + "|" + origem.getCnpj() + "|" + destino.getCnpj() + "|" + valoredPositivo.toPlainString());
    }

    private static BigDecimal obterTarifaTransferencia(String tipoTransferencia, String tipo, ContaPJ origem, BigDecimal valoredPositivo) {
        return switch (tipo) {
            case "PIX" -> DeterminarTarifa.executarTarifaPix(origem, valoredPositivo);
            case "TED" -> DeterminarTarifa.executarTarifaTed(origem);
            case "INTERNA" -> BigDecimal.ZERO;
            default -> throw new OperacaoInvalidaException("Tipo de transferencia desconhecido: " + tipoTransferencia);
        };
    }

    private void validarAntiFraude(BigDecimal valoredPositivo, ContaPJ origem) {
        if (valoredPositivo.compareTo(new BigDecimal("1000000.00")) > 0) {
            auditoriaObject.registrar("FRAUDE_SUSPEITA|" + origem.getCnpj() + "|" + valoredPositivo.toPlainString());
            throw new OperacaoInvalidaException("Transferencia bloqueada para analise antifraude");
        }
    }

    private BigDecimal validarTipoTransferencia(BigDecimal valor, String tipoTransferencia) {
        BigDecimal valoredPositivo = valorPositivo(valor);
        if (tipoTransferencia == null) {
            throw new OperacaoInvalidaException("Tipo de transferencia obrigatorio");
        }
        return valoredPositivo;
    }

    @Override
    public void pagarBoleto(String cnpj, String codigoBarras, BigDecimal valor) {
        ContaPJ conta = buscarAtiva(cnpj);
        BigDecimal valoredPositivo = valorPositivo(valor);
        validarCodigoBarra(codigoBarras);

        BigDecimal tarifa = DeterminarTarifa.executarTarifaBoleto(conta);
        tarifa = normalizar(tarifa);

        conta.validarSaldo(valoredPositivo.add(tarifa));

        efetivarPagamentoBoleto(tarifa, conta, "Tarifa de boleto", "BOLETO|", valoredPositivo);
    }

    private void efetivarPagamentoBoleto(BigDecimal tarifa, ContaPJ conta, String tipoTarifa, String x, BigDecimal valoredPositivo) {
        conta.debitar(valoredPositivo);
        conta.registrarLancamento("BOLETO", "Pagamento de boleto", valoredPositivo);

        if (tarifa.signum() > 0) {
            conta.debitar(tarifa);
            conta.registrarLancamento("TARIFA", tipoTarifa, tarifa);
        }
        auditoriaObject.registrar(x + conta.getCnpj() + "|" + valoredPositivo.toPlainString());
    }

    private static void validarCodigoBarra(String codigoBarras) {
        if (codigoBarras == null || !codigoBarras.matches("\\d{44}")) {
            throw new OperacaoInvalidaException("Codigo de barras invalido");
        }
    }

    // ------------------------------------------------------------------
    // Credito
    // ------------------------------------------------------------------

    @Override
    public Emprestimo solicitarEmprestimo(String cnpj, BigDecimal valor, int meses) {
        ContaPJ conta = buscarAtiva(cnpj);
        BigDecimal v = valorPositivo(valor);
        if (meses < 1 || meses > 60) {
            throw new OperacaoInvalidaException("Prazo deve ser entre 1 e 60 meses");
        }

        BigDecimal valorMaximo;
        BigDecimal taxa;
        int bonusTipo;
        int maxEmprestimos;
        switch (conta.getTipo()) {
            case MEI:
                valorMaximo = new BigDecimal("20000.00");
                taxa = new BigDecimal("0.035");
                bonusTipo = 0;
                maxEmprestimos = 1;
                break;
            case ME:
                valorMaximo = new BigDecimal("100000.00");
                taxa = new BigDecimal("0.028");
                bonusTipo = 50;
                maxEmprestimos = 3;
                break;
            case EPP:
                valorMaximo = new BigDecimal("500000.00");
                taxa = new BigDecimal("0.022");
                bonusTipo = 100;
                maxEmprestimos = 3;
                break;
            case LTDA:
                valorMaximo = new BigDecimal("2000000.00");
                taxa = new BigDecimal("0.018");
                bonusTipo = 150;
                maxEmprestimos = 3;
                break;
            default:
                valorMaximo = new BigDecimal("10000000.00");
                taxa = new BigDecimal("0.012");
                bonusTipo = 250;
                maxEmprestimos = 3;
        }

        if (v.compareTo(valorMaximo) > 0) {
            throw new OperacaoInvalidaException("Valor acima do maximo para o tipo de empresa");
        }
        if (conta.getEmprestimos().size() >= maxEmprestimos) {
            throw new OperacaoInvalidaException("Limite de emprestimos ativos atingido");
        }

        int score = 500 + bonusTipo;
        if (conta.getSaldo().signum() > 0) {
            int bonusSaldo = conta.getSaldo().divide(new BigDecimal("1000"), 0, RoundingMode.DOWN).intValue();
            score += Math.min(bonusSaldo, 200);
        } else if (conta.getSaldo().signum() < 0) {
            score -= 200;
        }
        if (score < 600) {
            throw new OperacaoInvalidaException("Credito negado: score insuficiente (" + score + ")");
        }

        BigDecimal fator = BigDecimal.ONE.add(taxa.multiply(BigDecimal.valueOf(meses)));
        BigDecimal total = normalizar(v.multiply(fator));
        BigDecimal parcela = total.divide(BigDecimal.valueOf(meses), 2, RoundingMode.HALF_UP);

        Emprestimo emprestimo = new Emprestimo(v, meses, taxa, parcela, total);
        conta.getEmprestimos().add(emprestimo);
        conta.setSaldo(conta.getSaldo().add(v));
        conta.getLancamentos().add(new Lancamento("EMPRESTIMO", "Credito de emprestimo", v));
        auditoria.add("EMPRESTIMO|" + conta.getCnpj() + "|" + v.toPlainString() + "|" + meses);
        email.enviar(conta.getEmail(), "Emprestimo aprovado", "Valor liberado: " + v.toPlainString());
        return emprestimo;
    }

    @Override
    public void solicitarAumentoLimite(String cnpj, BigDecimal novoLimite) {
        ContaPJ conta = buscarAtiva(cnpj);
        if (conta instanceof ContaMEI) {
            throw new OperacaoInvalidaException("Conta MEI nao possui cheque especial");
        }
        BigDecimal limite = valorPositivo(novoLimite);
        if (limite.compareTo(conta.getLimiteChequeEspecial()) <= 0) {
            throw new OperacaoInvalidaException("Novo limite deve ser maior que o atual");
        }

        BigDecimal maximo;
        if (conta.getTipo() == TipoEmpresa.ME) {
            maximo = new BigDecimal("20000.00");
        } else if (conta.getTipo() == TipoEmpresa.EPP) {
            maximo = new BigDecimal("80000.00");
        } else if (conta.getTipo() == TipoEmpresa.LTDA) {
            maximo = new BigDecimal("200000.00");
        } else {
            maximo = new BigDecimal("1000000.00");
        }
        if (limite.compareTo(maximo) > 0) {
            throw new OperacaoInvalidaException("Limite acima do maximo permitido");
        }

        conta.setLimiteChequeEspecial(limite);
        auditoria.add("AUMENTO_LIMITE|" + conta.getCnpj() + "|" + limite.toPlainString());
        email.enviar(conta.getEmail(), "Limite alterado", "Novo limite: " + limite.toPlainString());
    }

    // ------------------------------------------------------------------
    // Extrato e tarifas
    // ------------------------------------------------------------------

    @Override
    public String gerarExtrato(String cnpj, String formato) {
        ContaPJ conta = buscarAtiva(cnpj);
        if (formato == null) {
            throw new OperacaoInvalidaException("Formato obrigatorio");
        }
        String f = formato.trim().toUpperCase();
        StringBuilder sb = new StringBuilder();

        if (f.equals("TXT")) {
            sb.append("EXTRATO - ").append(conta.getRazaoSocial()).append(" (").append(conta.getCnpj()).append(")\n");
            for (Lancamento l : conta.getLancamentos()) {
                sb.append(l.tipo()).append(" | ").append(l.descricao()).append(" | ")
                  .append(normalizar(l.valor()).toPlainString()).append("\n");
            }
            sb.append("SALDO: ").append(normalizar(conta.getSaldo()).toPlainString()).append("\n");
        } else if (f.equals("CSV")) {
            sb.append("tipo;descricao;valor\n");
            for (Lancamento l : conta.getLancamentos()) {
                sb.append(l.tipo()).append(";").append(l.descricao()).append(";")
                  .append(normalizar(l.valor()).toPlainString()).append("\n");
            }
        } else if (f.equals("JSON")) {
            sb.append("{\"cnpj\":\"").append(conta.getCnpj()).append("\",");
            sb.append("\"razaoSocial\":\"").append(conta.getRazaoSocial().replace("\"", "\\\"")).append("\",");
            sb.append("\"saldo\":").append(normalizar(conta.getSaldo()).toPlainString()).append(",");
            sb.append("\"lancamentos\":[");
            for (int i = 0; i < conta.getLancamentos().size(); i++) {
                Lancamento l = conta.getLancamentos().get(i);
                if (i > 0) {
                    sb.append(",");
                }
                sb.append("{\"tipo\":\"").append(l.tipo()).append("\",");
                sb.append("\"descricao\":\"").append(l.descricao()).append("\",");
                sb.append("\"valor\":").append(normalizar(l.valor()).toPlainString()).append("}");
            }
            sb.append("]}");
        } else {
            throw new OperacaoInvalidaException("Formato de extrato nao suportado: " + formato);
        }
        return sb.toString();
    }

    @Override
    public BigDecimal cobrarTarifaMensal(String cnpj) {
        ContaPJ conta = buscarAtiva(cnpj);

        BigDecimal tarifa = BigDecimal.ZERO;
        tarifa = DeterminarTarifa.executarTarifaMensal(conta, tarifa);
        tarifa = normalizar(tarifa);

        if (tarifa.signum() > 0) {
            conta.debitar(tarifa);
            conta.registrarLancamento("TARIFA", "Tarifa mensal de manutencao", tarifa);
            auditoriaObject.registrar("TARIFA_MENSAL|" + conta.getCnpj() + "|" + tarifa.toPlainString());
        }
        return tarifa;
    }

    // ------------------------------------------------------------------
    // Consultas de apoio (notificacoes e auditoria)
    // ------------------------------------------------------------------

    public List<String> getEmailsEnviados() {
        return email.getEnviados();
    }

    public List<String> getSmsEnviados() {
        return sms.getEnviados();
    }

    public List<String> getAuditoria() {
        return auditoriaObject.getAuditorias();
    }

    // ------------------------------------------------------------------
    // Auxiliares
    // ------------------------------------------------------------------

    private ContaPJ buscarAtiva(String cnpj) {
        if (cnpj == null) {
            throw new ContaNaoEncontradaException("CNPJ nao informado");
        }
        ContaPJ conta = repositorio.buscarPorCnpj(ValidadorCnpj.somenteDigitos(cnpj));
        if (conta == null) {
            throw new ContaNaoEncontradaException("Conta nao encontrada: " + cnpj);
        }
        if (!conta.isAtiva()) {
            throw new OperacaoInvalidaException("Conta encerrada");
        }
        return conta;
    }

    private BigDecimal valorPositivo(BigDecimal valor) {
        if (valor == null) {
            throw new OperacaoInvalidaException("Valor obrigatorio");
        }
        BigDecimal v = normalizar(valor);
        if (v.signum() <= 0) {
            throw new OperacaoInvalidaException("Valor deve ser maior que zero");
        }
        return v;
    }

    private BigDecimal normalizar(BigDecimal valor) {
        return valor.setScale(2, RoundingMode.HALF_UP);
    }
}
