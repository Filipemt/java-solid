package com.banco.pj.servico.impl;

import com.banco.pj.dominio.*;
import com.banco.pj.dominio.CondicoesEmprestimo;
import com.banco.pj.dominio.enums.TipoEmpresa;
import com.banco.pj.dominio.exceptions.ContaNaoEncontradaException;
import com.banco.pj.dominio.exceptions.OperacaoInvalidaException;
import com.banco.pj.infra.*;
import com.banco.pj.servico.OperacoesContaPJ;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

public class ServicoContaPJ implements OperacoesContaPJ {

    private final RepositorioContasEmMemoria repositorio;

    private final EnviadorEmail email = new EnviadorEmail();
    private final EnviadorSms sms = new EnviadorSms();

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
        BigDecimal valoredPositivo = valorPositivo(valor);
        validarPrazoEmprestimo(meses);

        CondicoesEmprestimo condicoes = CondicoesEmprestimo.paraTipoEmpresa(conta.getTipo());
        validarElegibilidade(valoredPositivo, condicoes, conta);
        validarScore(condicoes, conta);

        Emprestimo emprestimo = Emprestimo.calcular(valoredPositivo, meses, condicoes.taxa());
        efetivarEmprestimo(meses, conta, emprestimo, valoredPositivo);

        notificarEmprestimoAprovado(conta, valoredPositivo);
        return emprestimo;
    }

    private void notificarEmprestimoAprovado(ContaPJ conta, BigDecimal valoredPositivo) {
        email.enviar(conta.getEmail(), "Emprestimo aprovado", "Valor liberado: " + valoredPositivo.toPlainString());
    }

    private void efetivarEmprestimo(int meses, ContaPJ conta, Emprestimo emprestimo, BigDecimal valoredPositivo) {
        conta.getEmprestimos().add(emprestimo);
        conta.creditar(valoredPositivo);
        conta.registrarLancamento("EMPRESTIMO", "Credito de emprestimo", valoredPositivo);
        auditoriaObject.registrar("EMPRESTIMO|" + conta.getCnpj() + "|" + valoredPositivo.toPlainString() + "|" + meses);
    }

    private static void validarScore(CondicoesEmprestimo condicoes, ContaPJ conta) {
        int score = 500 + condicoes.bonusTipo();
        if (conta.getSaldo().signum() > 0) {
            int bonusSaldo = conta.getSaldo().divide(new BigDecimal("1000"), 0, RoundingMode.DOWN).intValue();
            score += Math.min(bonusSaldo, 200);
        } else if (conta.getSaldo().signum() < 0) {
            score -= 200;
        }
        if (score < 600) {
            throw new OperacaoInvalidaException("Credito negado: score insuficiente (" + score + ")");
        }
    }

    private static void validarElegibilidade(BigDecimal v, CondicoesEmprestimo condicoes, ContaPJ conta) {
        if (v.compareTo(condicoes.valorMaximo()) > 0) {
            throw new OperacaoInvalidaException("Valor acima do maximo para o tipo de empresa");
        }
        if (conta.getEmprestimos().size() >= condicoes.maxEmprestimos()) {
            throw new OperacaoInvalidaException("Limite de emprestimos ativos atingido");
        }
    }

    private static void validarPrazoEmprestimo(int meses) {
        if (meses < 1 || meses > 60) {
            throw new OperacaoInvalidaException("Prazo deve ser entre 1 e 60 meses");
        }
    }

    @Override
    public void solicitarAumentoLimite(String cnpj, BigDecimal novoLimite) {
        ContaPJ conta = buscarAtiva(cnpj);
        validarContaElegivelAChequeEspecial(conta);
        BigDecimal limite = validarNovoLimite(novoLimite, conta);
        CondicoesLimiteChequeEspecial condicoes = CondicoesLimiteChequeEspecial.paraTipoEmpresa(conta.getTipo());
        validarLimiteMaximo(limite, condicoes);

        efetivarAumentoLimite(conta, limite);
        notificarLimiteAlterado(conta, limite);
    }

    private void efetivarAumentoLimite(ContaPJ conta, BigDecimal limite) {
        conta.setLimiteChequeEspecial(limite);
        auditoriaObject.registrar("AUMENTO_LIMITE|" + conta.getCnpj() + "|" + limite.toPlainString());
    }

    private void notificarLimiteAlterado(ContaPJ conta, BigDecimal limite) {
        email.enviar(conta.getEmail(), "Limite alterado", "Novo limite: " + limite.toPlainString());
    }

    private static void validarContaElegivelAChequeEspecial(ContaPJ conta) {
        if (conta instanceof ContaMEI) {
            throw new OperacaoInvalidaException("Conta MEI nao possui cheque especial");
        }
    }

    private BigDecimal validarNovoLimite(BigDecimal novoLimite, ContaPJ conta) {
        BigDecimal limite = valorPositivo(novoLimite);
        if (limite.compareTo(conta.getLimiteChequeEspecial()) <= 0) {
            throw new OperacaoInvalidaException("Novo limite deve ser maior que o atual");
        }
        return limite;
    }

    private static void validarLimiteMaximo(BigDecimal limite, CondicoesLimiteChequeEspecial condicoes) {
        if (limite.compareTo(condicoes.limiteMaximo()) > 0) {
            throw new OperacaoInvalidaException("Limite acima do maximo permitido");
        }
    }

    // ------------------------------------------------------------------
    // Extrato e tarifas
    // ------------------------------------------------------------------

    @Override
    public String gerarExtrato(String cnpj, String formato) {
        ContaPJ conta = buscarAtiva(cnpj);
        validarFormatoObrigatorio(formato);
        String formatoNormalizado = normalizarFormato(formato);
        validarFormatoSuportado(formatoNormalizado, formato);
        return ExtratoGerado.paraFormato(formatoNormalizado, conta).conteudo();
    }

    private static void validarFormatoObrigatorio(String formato) {
        if (formato == null) {
            throw new OperacaoInvalidaException("Formato obrigatorio");
        }
    }

    private static String normalizarFormato(String formato) {
        return formato.trim().toUpperCase();
    }

    private static void validarFormatoSuportado(String formatoNormalizado, String formatoOriginal) {
        if (!formatoNormalizado.equals("TXT")
                && !formatoNormalizado.equals("CSV")
                && !formatoNormalizado.equals("JSON")) {
            throw new OperacaoInvalidaException("Formato de extrato nao suportado: " + formatoOriginal);
        }
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
