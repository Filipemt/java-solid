package com.banco.pj.servico;

import com.banco.pj.dominio.*;
import com.banco.pj.dominio.enums.TipoEmpresa;
import com.banco.pj.dominio.exceptions.ContaNaoEncontradaException;
import com.banco.pj.dominio.exceptions.OperacaoInvalidaException;
import com.banco.pj.dominio.exceptions.SaldoInsuficienteException;
import com.banco.pj.infra.EnviadorEmail;
import com.banco.pj.infra.EnviadorSms;
import com.banco.pj.infra.RepositorioContasEmMemoria;
import com.banco.pj.servico.impl.AberturaContaValidador;
import com.banco.pj.servico.impl.CaixaEletronicoPJ;
import com.banco.pj.servico.impl.PoliticaEmprestimoImpl;
import com.banco.pj.servico.impl.ServicoContaPJ;
import com.banco.pj.servico.impl.strategy.SeletorPoliticaTarifacao;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Suite de regressao do servico de conta PJ.
 *
 * Contrato: os testes so conversam com a API publica do servico (e do caixa eletronico).
 * A unica linha que pode precisar mudar numa refatoracao e a criacao do servico em {@link #criarServico()}.
 */
class ServicoContaPJTest {

    static final String CNPJ_A = "11444777000161";
    static final String CNPJ_B = "11222333000181";

    ServicoContaPJ servico;

    @BeforeEach
    void setUp() {
        servico = criarServico();
    }

    ServicoContaPJ criarServico() {
        RepositorioContasEmMemoria repositorio = new RepositorioContasEmMemoria();
        AberturaContaValidador aberturaContaValidador = new AberturaContaValidador(repositorio);
        Auditoria auditoriaObject = new Auditoria();
        EnviadorEmail email = new EnviadorEmail();
        EnviadorSms sms = new EnviadorSms();
        PoliticaEmprestimo politicaEmprestimo = new PoliticaEmprestimoImpl();


        return new ServicoContaPJ(repositorio, email, sms, aberturaContaValidador, auditoriaObject, SeletorPoliticaTarifacao.padrao(), politicaEmprestimo);
    }

    // ---------------- helpers ----------------

    void abrir(String cnpj, TipoEmpresa tipo, String deposito) {
        abrirEm(servico, cnpj, tipo, deposito);
    }

    static void abrirEm(ServicoContaPJ s, String cnpj, TipoEmpresa tipo, String deposito) {
        s.abrirConta(cnpj, "Empresa " + cnpj, tipo, "contato@empresa.com.br", "61999990000", new BigDecimal(deposito));
    }

    void assertSaldo(String cnpj, String esperado) {
        assertValor(esperado, servico.consultarSaldo(cnpj));
    }

    static void assertValor(String esperado, BigDecimal atual) {
        assertEquals(0, new BigDecimal(esperado).compareTo(atual),
                "Valor esperado " + esperado + " mas foi " + atual);
    }

    // =====================================================================
    @Nested
    class AberturaDeConta {

        @Test
        void abreContaComDepositoInicial() {
            abrir(CNPJ_A, TipoEmpresa.ME, "1000.00");
            assertSaldo(CNPJ_A, "1000.00");
        }

        @Test
        void rejeitaCnpjInvalido() {
            assertThrows(OperacaoInvalidaException.class, () -> abrir("11444777000162", TipoEmpresa.ME, "0"));
            assertThrows(OperacaoInvalidaException.class, () -> abrir("11111111111111", TipoEmpresa.ME, "0"));
            assertThrows(OperacaoInvalidaException.class, () -> abrir("123", TipoEmpresa.ME, "0"));
            assertThrows(OperacaoInvalidaException.class, () -> abrir(null, TipoEmpresa.ME, "0"));
        }

        @Test
        void aceitaCnpjFormatado() {
            abrir("11.444.777/0001-61", TipoEmpresa.ME, "100");
            assertSaldo(CNPJ_A, "100.00");
            assertSaldo("11.444.777/0001-61", "100.00");
        }

        @Test
        void rejeitaContaDuplicada() {
            abrir(CNPJ_A, TipoEmpresa.ME, "0");
            assertThrows(OperacaoInvalidaException.class, () -> abrir(CNPJ_A, TipoEmpresa.LTDA, "5000"));
        }

        @Test
        void rejeitaRazaoSocialEmBranco() {
            assertThrows(OperacaoInvalidaException.class, () ->
                    servico.abrirConta(CNPJ_A, "   ", TipoEmpresa.ME, "a@b.com", "6199", BigDecimal.ZERO));
        }

        @Test
        void rejeitaEmailInvalido() {
            assertThrows(OperacaoInvalidaException.class, () ->
                    servico.abrirConta(CNPJ_A, "Empresa", TipoEmpresa.ME, "email-sem-arroba", "6199", BigDecimal.ZERO));
        }

        @Test
        void rejeitaTipoNulo() {
            assertThrows(OperacaoInvalidaException.class, () ->
                    servico.abrirConta(CNPJ_A, "Empresa", null, "a@b.com", "6199", BigDecimal.ZERO));
        }

        @Test
        void rejeitaDepositoInicialNegativo() {
            assertThrows(OperacaoInvalidaException.class, () -> abrir(CNPJ_A, TipoEmpresa.ME, "-1"));
        }

        @ParameterizedTest
        @CsvSource({"EPP,500.00", "LTDA,1000.00", "SA,10000.00"})
        void respeitaDepositoMinimoPorTipo(TipoEmpresa tipo, String minimo) {
            String abaixo = new BigDecimal(minimo).subtract(new BigDecimal("0.01")).toPlainString();
            assertThrows(OperacaoInvalidaException.class, () -> abrir(CNPJ_A, tipo, abaixo));
            assertDoesNotThrow(() -> abrir(CNPJ_A, tipo, minimo));
        }

        @Test
        void enviaEmailDeBoasVindas() {
            abrir(CNPJ_A, TipoEmpresa.ME, "0");
            assertEquals(1, servico.getEmailsEnviados().size());
            assertTrue(servico.getEmailsEnviados().get(0).contains("Bem-vindo"));
        }

        @Test
        void aberturaNaoDisparaSms() {
            abrir(CNPJ_A, TipoEmpresa.SA, "500000");
            assertTrue(servico.getSmsEnviados().isEmpty());
        }

        @Test
        void contaInexistenteLancaExcecao() {
            assertThrows(ContaNaoEncontradaException.class, () -> servico.consultarSaldo("00000000000191"));
            assertThrows(ContaNaoEncontradaException.class, () -> servico.consultarSaldo(null));
        }
    }

    // =====================================================================
    @Nested
    class EncerramentoDeConta {

        @Test
        void encerraContaComSaldoZero() {
            abrir(CNPJ_A, TipoEmpresa.ME, "0");
            servico.encerrarConta(CNPJ_A);
            assertThrows(OperacaoInvalidaException.class, () -> servico.consultarSaldo(CNPJ_A));
            assertTrue(servico.getEmailsEnviados().stream().anyMatch(e -> e.contains("Conta encerrada")));
        }

        @Test
        void naoEncerraComSaldoPositivoOuNegativo() {
            abrir(CNPJ_A, TipoEmpresa.ME, "10");
            assertThrows(OperacaoInvalidaException.class, () -> servico.encerrarConta(CNPJ_A));

            abrir(CNPJ_B, TipoEmpresa.ME, "0");
            servico.sacar(CNPJ_B, new BigDecimal("100"));
            assertThrows(OperacaoInvalidaException.class, () -> servico.encerrarConta(CNPJ_B));
        }

        @Test
        void contaEncerradaNaoAceitaOperacoes() {
            abrir(CNPJ_A, TipoEmpresa.ME, "0");
            servico.encerrarConta(CNPJ_A);
            assertThrows(OperacaoInvalidaException.class, () -> servico.depositar(CNPJ_A, BigDecimal.TEN));
            assertThrows(OperacaoInvalidaException.class, () -> servico.sacar(CNPJ_A, BigDecimal.TEN));
        }

        @Test
        void naoPermiteReabrirContaEncerrada() {
            abrir(CNPJ_A, TipoEmpresa.ME, "0");
            servico.encerrarConta(CNPJ_A);
            assertThrows(OperacaoInvalidaException.class, () -> abrir(CNPJ_A, TipoEmpresa.ME, "0"));
        }
    }

    // =====================================================================
    @Nested
    class Deposito {

        @Test
        void depositoAumentaSaldo() {
            abrir(CNPJ_A, TipoEmpresa.ME, "100");
            servico.depositar(CNPJ_A, new BigDecimal("250.55"));
            assertSaldo(CNPJ_A, "350.55");
        }

        @Test
        void rejeitaValorZeroNegativoOuNulo() {
            abrir(CNPJ_A, TipoEmpresa.ME, "100");
            assertThrows(OperacaoInvalidaException.class, () -> servico.depositar(CNPJ_A, BigDecimal.ZERO));
            assertThrows(OperacaoInvalidaException.class, () -> servico.depositar(CNPJ_A, new BigDecimal("-5")));
            assertThrows(OperacaoInvalidaException.class, () -> servico.depositar(CNPJ_A, null));
            assertSaldo(CNPJ_A, "100.00");
        }

        @Test
        void depositoAltoGeraAlertaCoafESms() {
            abrir(CNPJ_A, TipoEmpresa.ME, "0");
            servico.depositar(CNPJ_A, new BigDecimal("10000.00"));
            assertTrue(servico.getAuditoria().stream().anyMatch(a -> a.startsWith("ALERTA_COAF")));
            assertEquals(1, servico.getSmsEnviados().size());
        }

        @Test
        void depositoAbaixoDoLimiteNaoGeraAlerta() {
            abrir(CNPJ_A, TipoEmpresa.ME, "0");
            servico.depositar(CNPJ_A, new BigDecimal("9999.99"));
            assertTrue(servico.getAuditoria().stream().noneMatch(a -> a.startsWith("ALERTA_COAF")));
            assertTrue(servico.getSmsEnviados().isEmpty());
        }
    }

    // =====================================================================
    @Nested
    class Saque {

        @ParameterizedTest
        @CsvSource({"MEI,1000,0.00", "ME,1000,1.50", "EPP,1000,1.00", "LTDA,1000,0.50", "SA,10000,0.00"})
        void cobraTarifaDeSaquePorTipo(TipoEmpresa tipo, String deposito, String tarifa) {
            abrir(CNPJ_A, tipo, deposito);
            servico.sacar(CNPJ_A, new BigDecimal("100"));
            BigDecimal esperado = new BigDecimal(deposito).subtract(new BigDecimal("100")).subtract(new BigDecimal(tarifa));
            assertSaldo(CNPJ_A, esperado.toPlainString());
        }

        @ParameterizedTest
        @CsvSource({
                "MEI,100,0,0.00",
                "ME,0,5000,1.50",
                "EPP,500,20000,1.00",
                "LTDA,1000,50000,0.50",
                "SA,10000,200000,0.00"
        })
        void usaChequeEspecialAteOLimiteDoTipo(TipoEmpresa tipo, String deposito, String limite, String tarifa) {
            BigDecimal maximo = new BigDecimal(deposito).add(new BigDecimal(limite)).subtract(new BigDecimal(tarifa));

            abrir(CNPJ_A, tipo, deposito);
            servico.sacar(CNPJ_A, maximo);
            assertSaldo(CNPJ_A, new BigDecimal(limite).negate().toPlainString());

            ServicoContaPJ outro = criarServico();
            abrirEm(outro, CNPJ_B, tipo, deposito);
            BigDecimal acima = maximo.add(new BigDecimal("0.01"));
            assertThrows(SaldoInsuficienteException.class, () -> outro.sacar(CNPJ_B, acima));
        }

        @Test
        void saqueNegadoNaoAlteraSaldo() {
            abrir(CNPJ_A, TipoEmpresa.ME, "100");
            assertThrows(SaldoInsuficienteException.class, () -> servico.sacar(CNPJ_A, new BigDecimal("5100")));
            assertSaldo(CNPJ_A, "100.00");
        }

        @Test
        void rejeitaValorInvalido() {
            abrir(CNPJ_A, TipoEmpresa.ME, "100");
            assertThrows(OperacaoInvalidaException.class, () -> servico.sacar(CNPJ_A, BigDecimal.ZERO));
            assertThrows(OperacaoInvalidaException.class, () -> servico.sacar(CNPJ_A, new BigDecimal("-1")));
        }
    }

    // =====================================================================
    @Nested
    class Transferencia {

        @BeforeEach
        void abrirContasBase() {
            abrir(CNPJ_A, TipoEmpresa.ME, "10000");
            abrir(CNPJ_B, TipoEmpresa.MEI, "0");
        }

        @Test
        void pixNaoTemTarifa() {
            servico.transferir(CNPJ_A, CNPJ_B, new BigDecimal("1000"), "PIX");
            assertSaldo(CNPJ_A, "9000.00");
            assertSaldo(CNPJ_B, "1000.00");
        }

        @Test
        void transferenciaInternaNaoTemTarifa() {
            servico.transferir(CNPJ_A, CNPJ_B, new BigDecimal("200"), "INTERNA");
            assertSaldo(CNPJ_A, "9800.00");
            assertSaldo(CNPJ_B, "200.00");
        }

        @Test
        void tipoDeTransferenciaNaoDependeDeCaixaAltaOuBaixa() {
            servico.transferir(CNPJ_A, CNPJ_B, new BigDecimal("100"), "pix");
            assertSaldo(CNPJ_B, "100.00");
        }

        @Test
        void rejeitaTipoDesconhecidoOuNulo() {
            assertThrows(OperacaoInvalidaException.class, () -> servico.transferir(CNPJ_A, CNPJ_B, BigDecimal.TEN, "DOC"));
            assertThrows(OperacaoInvalidaException.class, () -> servico.transferir(CNPJ_A, CNPJ_B, BigDecimal.TEN, null));
        }

        @Test
        void rejeitaOrigemIgualDestino() {
            assertThrows(OperacaoInvalidaException.class, () -> servico.transferir(CNPJ_A, CNPJ_A, BigDecimal.TEN, "PIX"));
        }

        @Test
        void destinoInexistenteNaoAlteraOrigem() {
            assertThrows(ContaNaoEncontradaException.class,
                    () -> servico.transferir(CNPJ_A, "00000000000191", BigDecimal.TEN, "PIX"));
            assertSaldo(CNPJ_A, "10000.00");
        }

        @Test
        void saldoInsuficienteNaoAlteraNenhumaConta() {
            assertThrows(SaldoInsuficienteException.class,
                    () -> servico.transferir(CNPJ_B, CNPJ_A, new BigDecimal("0.01"), "PIX"));
            assertSaldo(CNPJ_A, "10000.00");
            assertSaldo(CNPJ_B, "0.00");
        }

        @Test
        void rejeitaValorInvalido() {
            assertThrows(OperacaoInvalidaException.class, () -> servico.transferir(CNPJ_A, CNPJ_B, BigDecimal.ZERO, "PIX"));
        }

        @Test
        void transferenciaGeraLancamentosNasDuasContas() {
            servico.transferir(CNPJ_A, CNPJ_B, new BigDecimal("100"), "PIX");
            assertTrue(servico.gerarExtrato(CNPJ_A, "CSV").contains("TRANSFERENCIA_ENVIADA"));
            assertTrue(servico.gerarExtrato(CNPJ_B, "CSV").contains("TRANSFERENCIA_RECEBIDA"));
        }

        @Test
        void transferenciaAltaGeraSms() {
            abrir(CNPJ_C_GRANDE, TipoEmpresa.ME, "100000");
            servico.transferir(CNPJ_C_GRANDE, CNPJ_B, new BigDecimal("50000.00"), "INTERNA");
            assertEquals(1, servico.getSmsEnviados().size());
        }

        @Test
        void transferenciaAbaixoDoLimiteNaoGeraSms() {
            abrir(CNPJ_C_GRANDE, TipoEmpresa.ME, "100000");
            servico.transferir(CNPJ_C_GRANDE, CNPJ_B, new BigDecimal("49999.99"), "INTERNA");
            assertTrue(servico.getSmsEnviados().isEmpty());
        }
    }

    static final String CNPJ_C_GRANDE = "45723174000110";

    @Nested
    class TransferenciaPorTipoDeEmpresa {

        @ParameterizedTest
        @CsvSource({"MEI,4.75", "ME,9.50", "EPP,9.50", "LTDA,8.00", "SA,6.00"})
        void cobraTarifaDeTedPorTipo(TipoEmpresa tipo, String tarifa) {
            abrir(CNPJ_A, tipo, "50000");
            abrir(CNPJ_B, TipoEmpresa.ME, "0");
            servico.transferir(CNPJ_A, CNPJ_B, new BigDecimal("1000"), "TED");
            BigDecimal esperado = new BigDecimal("49000").subtract(new BigDecimal(tarifa));
            assertSaldo(CNPJ_A, esperado.toPlainString());
            assertSaldo(CNPJ_B, "1000.00");
        }

        @ParameterizedTest
        @CsvSource({"MEI,5000", "ME,20000", "EPP,50000", "LTDA,100000", "SA,500000"})
        void pixRespeitaLimitePorTipo(TipoEmpresa tipo, String limite) {
            abrir(CNPJ_A, tipo, "1000000");
            abrir(CNPJ_B, TipoEmpresa.ME, "0");
            servico.transferir(CNPJ_A, CNPJ_B, new BigDecimal(limite), "PIX");
            BigDecimal esperado = new BigDecimal("1000000").subtract(new BigDecimal(limite));
            assertSaldo(CNPJ_A, esperado.toPlainString());

            BigDecimal acima = new BigDecimal(limite).add(new BigDecimal("0.01"));
            assertThrows(OperacaoInvalidaException.class, () -> servico.transferir(CNPJ_A, CNPJ_B, acima, "PIX"));
        }

        @Test
        void tedAcimaDoLimiteDePixEPermitida() {
            abrir(CNPJ_A, TipoEmpresa.MEI, "100000");
            abrir(CNPJ_B, TipoEmpresa.ME, "0");
            servico.transferir(CNPJ_A, CNPJ_B, new BigDecimal("50000"), "TED");
            assertSaldo(CNPJ_B, "50000.00");
        }

        @Test
        void bloqueiaTransferenciaSuspeitaDeFraude() {
            abrir(CNPJ_A, TipoEmpresa.SA, "2000000");
            abrir(CNPJ_B, TipoEmpresa.ME, "0");
            assertThrows(OperacaoInvalidaException.class,
                    () -> servico.transferir(CNPJ_A, CNPJ_B, new BigDecimal("1000000.01"), "TED"));
            assertTrue(servico.getAuditoria().stream().anyMatch(a -> a.startsWith("FRAUDE_SUSPEITA")));
            assertSaldo(CNPJ_A, "2000000.00");
        }

        @Test
        void permiteTransferenciaNoLimiteAntifraude() {
            abrir(CNPJ_A, TipoEmpresa.SA, "2000000");
            abrir(CNPJ_B, TipoEmpresa.ME, "0");
            servico.transferir(CNPJ_A, CNPJ_B, new BigDecimal("1000000.00"), "TED");
            assertSaldo(CNPJ_A, "999994.00");
        }
    }

    // =====================================================================
    @Nested
    class Boleto {

        static final String CODIGO = "12345678901234567890123456789012345678901234";

        @ParameterizedTest
        @CsvSource({"MEI,0.00", "ME,2.00", "EPP,2.00", "LTDA,2.00", "SA,1.00"})
        void cobraTarifaDeBoletoPorTipo(TipoEmpresa tipo, String tarifa) {
            abrir(CNPJ_A, tipo, "10000");
            servico.pagarBoleto(CNPJ_A, CODIGO, new BigDecimal("500"));
            BigDecimal esperado = new BigDecimal("9500").subtract(new BigDecimal(tarifa));
            assertSaldo(CNPJ_A, esperado.toPlainString());
        }

        @Test
        void rejeitaCodigoDeBarrasInvalido() {
            abrir(CNPJ_A, TipoEmpresa.ME, "10000");
            assertThrows(OperacaoInvalidaException.class, () -> servico.pagarBoleto(CNPJ_A, CODIGO.substring(1), BigDecimal.TEN));
            assertThrows(OperacaoInvalidaException.class, () -> servico.pagarBoleto(CNPJ_A, "A" + CODIGO.substring(1), BigDecimal.TEN));
            assertThrows(OperacaoInvalidaException.class, () -> servico.pagarBoleto(CNPJ_A, null, BigDecimal.TEN));
            assertSaldo(CNPJ_A, "10000.00");
        }

        @Test
        void rejeitaBoletoComSaldoInsuficiente() {
            abrir(CNPJ_A, TipoEmpresa.MEI, "100");
            assertThrows(SaldoInsuficienteException.class, () -> servico.pagarBoleto(CNPJ_A, CODIGO, new BigDecimal("100.01")));
            assertSaldo(CNPJ_A, "100.00");
        }
    }

    // =====================================================================
    @Nested
    class Emprestimo_ {

        @Test
        void aprovaEmprestimoECalculaParcela() {
            abrir(CNPJ_A, TipoEmpresa.ME, "50000");
            Emprestimo e = servico.solicitarEmprestimo(CNPJ_A, new BigDecimal("10000"), 12);
            assertValor("10000.00", e.valorContratado());
            assertEquals(12, e.meses());
            assertValor("0.028", e.taxaMensal());
            assertValor("13360.00", e.totalAPagar());
            assertValor("1113.33", e.valorParcela());
            assertSaldo(CNPJ_A, "60000.00");
        }

        @ParameterizedTest
        @CsvSource({"MEI,0.035,1420.00", "ME,0.028,1336.00", "EPP,0.022,1264.00", "LTDA,0.018,1216.00", "SA,0.012,1144.00"})
        void taxaDependeDoTipoDeEmpresa(TipoEmpresa tipo, String taxa, String total) {
            abrir(CNPJ_A, tipo, "300000");
            Emprestimo e = servico.solicitarEmprestimo(CNPJ_A, new BigDecimal("1000"), 12);
            assertValor(taxa, e.taxaMensal());
            assertValor(total, e.totalAPagar());
        }

        @Test
        void negaCreditoQuandoScoreInsuficiente() {
            abrir(CNPJ_A, TipoEmpresa.ME, "0");
            assertThrows(OperacaoInvalidaException.class, () -> servico.solicitarEmprestimo(CNPJ_A, new BigDecimal("1000"), 12));
        }

        @Test
        void scoreNoLimiteDeAprovacao() {
            abrir(CNPJ_A, TipoEmpresa.ME, "49999.99");
            assertThrows(OperacaoInvalidaException.class, () -> servico.solicitarEmprestimo(CNPJ_A, new BigDecimal("1000"), 12));

            abrir(CNPJ_B, TipoEmpresa.ME, "50000.00");
            assertDoesNotThrow(() -> servico.solicitarEmprestimo(CNPJ_B, new BigDecimal("1000"), 12));
        }

        @Test
        void negaCreditoParaContaNegativada() {
            abrir(CNPJ_A, TipoEmpresa.SA, "10000");
            servico.sacar(CNPJ_A, new BigDecimal("20000"));
            assertThrows(OperacaoInvalidaException.class, () -> servico.solicitarEmprestimo(CNPJ_A, new BigDecimal("1000"), 12));
        }

        @ParameterizedTest
        @CsvSource({"MEI,20000", "ME,100000", "EPP,500000", "LTDA,2000000", "SA,10000000"})
        void respeitaValorMaximoPorTipo(TipoEmpresa tipo, String maximo) {
            abrir(CNPJ_A, tipo, "300000");
            BigDecimal acima = new BigDecimal(maximo).add(new BigDecimal("0.01"));
            assertThrows(OperacaoInvalidaException.class, () -> servico.solicitarEmprestimo(CNPJ_A, acima, 12));
            assertDoesNotThrow(() -> servico.solicitarEmprestimo(CNPJ_A, new BigDecimal(maximo), 12));
        }

        @Test
        void meiPodeTerApenasUmEmprestimo() {
            abrir(CNPJ_A, TipoEmpresa.MEI, "300000");
            servico.solicitarEmprestimo(CNPJ_A, new BigDecimal("1000"), 6);
            assertThrows(OperacaoInvalidaException.class, () -> servico.solicitarEmprestimo(CNPJ_A, new BigDecimal("1000"), 6));
        }

        @Test
        void demaisTiposPodemTerAteTresEmprestimos() {
            abrir(CNPJ_A, TipoEmpresa.ME, "300000");
            servico.solicitarEmprestimo(CNPJ_A, new BigDecimal("1000"), 6);
            servico.solicitarEmprestimo(CNPJ_A, new BigDecimal("1000"), 6);
            servico.solicitarEmprestimo(CNPJ_A, new BigDecimal("1000"), 6);
            assertThrows(OperacaoInvalidaException.class, () -> servico.solicitarEmprestimo(CNPJ_A, new BigDecimal("1000"), 6));
        }

        @Test
        void rejeitaPrazoInvalido() {
            abrir(CNPJ_A, TipoEmpresa.ME, "300000");
            assertThrows(OperacaoInvalidaException.class, () -> servico.solicitarEmprestimo(CNPJ_A, new BigDecimal("1000"), 0));
            assertThrows(OperacaoInvalidaException.class, () -> servico.solicitarEmprestimo(CNPJ_A, new BigDecimal("1000"), -1));
            assertThrows(OperacaoInvalidaException.class, () -> servico.solicitarEmprestimo(CNPJ_A, new BigDecimal("1000"), 61));
            assertDoesNotThrow(() -> servico.solicitarEmprestimo(CNPJ_A, new BigDecimal("1000"), 60));
        }

        @Test
        void rejeitaValorInvalido() {
            abrir(CNPJ_A, TipoEmpresa.ME, "300000");
            assertThrows(OperacaoInvalidaException.class, () -> servico.solicitarEmprestimo(CNPJ_A, BigDecimal.ZERO, 12));
        }

        @Test
        void enviaEmailDeAprovacao() {
            abrir(CNPJ_A, TipoEmpresa.ME, "50000");
            servico.solicitarEmprestimo(CNPJ_A, new BigDecimal("1000"), 12);
            assertTrue(servico.getEmailsEnviados().stream().anyMatch(e -> e.contains("Emprestimo aprovado")));
        }
    }

    // =====================================================================
    @Nested
    class AumentoDeLimite {

        @Test
        void aumentaLimiteEPassaAUsarNovoValor() {
            abrir(CNPJ_A, TipoEmpresa.ME, "0");
            servico.solicitarAumentoLimite(CNPJ_A, new BigDecimal("10000"));
            servico.sacar(CNPJ_A, new BigDecimal("9998.50"));
            assertSaldo(CNPJ_A, "-10000.00");
        }

        @Test
        void respeitaLimiteMaximoDoTipo() {
            abrir(CNPJ_A, TipoEmpresa.ME, "0");
            assertThrows(OperacaoInvalidaException.class, () -> servico.solicitarAumentoLimite(CNPJ_A, new BigDecimal("20000.01")));
            assertDoesNotThrow(() -> servico.solicitarAumentoLimite(CNPJ_A, new BigDecimal("20000.00")));
        }

        @Test
        void meiNaoPodeTerChequeEspecial() {
            abrir(CNPJ_A, TipoEmpresa.MEI, "0");
            assertThrows(OperacaoInvalidaException.class, () -> servico.solicitarAumentoLimite(CNPJ_A, new BigDecimal("1000")));
        }

        @Test
        void novoLimiteDeveSerMaiorQueOAtual() {
            abrir(CNPJ_A, TipoEmpresa.ME, "0");
            assertThrows(OperacaoInvalidaException.class, () -> servico.solicitarAumentoLimite(CNPJ_A, new BigDecimal("5000")));
            assertThrows(OperacaoInvalidaException.class, () -> servico.solicitarAumentoLimite(CNPJ_A, new BigDecimal("100")));
        }
    }

    // =====================================================================
    @Nested
    class Extrato {

        void montarMovimentacao() {
            servico.abrirConta(CNPJ_A, "Padaria Pao Quente", TipoEmpresa.ME, "a@b.com", "6199", new BigDecimal("1000"));
            servico.depositar(CNPJ_A, new BigDecimal("500"));
            servico.sacar(CNPJ_A, new BigDecimal("100"));
        }

        @Test
        void geraExtratoCsv() {
            montarMovimentacao();
            String[] linhas = servico.gerarExtrato(CNPJ_A, "CSV").split("\n");
            assertEquals(5, linhas.length);
            assertEquals("tipo;descricao;valor", linhas[0]);
            assertEquals("DEPOSITO;Deposito inicial;1000.00", linhas[1]);
            assertEquals("DEPOSITO;Deposito em conta;500.00", linhas[2]);
            assertEquals("SAQUE;Saque em conta;100.00", linhas[3]);
            assertEquals("TARIFA;Tarifa de saque;1.50", linhas[4]);
        }

        @Test
        void geraExtratoTxt() {
            montarMovimentacao();
            String txt = servico.gerarExtrato(CNPJ_A, "TXT");
            assertTrue(txt.startsWith("EXTRATO - Padaria Pao Quente (11444777000161)\n"));
            assertTrue(txt.contains("DEPOSITO | Deposito inicial | 1000.00\n"));
            assertTrue(txt.contains("SAQUE | Saque em conta | 100.00\n"));
            assertTrue(txt.endsWith("SALDO: 1398.50\n"));
        }

        @Test
        void geraExtratoJson() {
            servico.abrirConta(CNPJ_A, "Padaria Pao Quente", TipoEmpresa.ME, "a@b.com", "6199", new BigDecimal("1000"));
            servico.depositar(CNPJ_A, new BigDecimal("500"));
            String esperado = "{\"cnpj\":\"11444777000161\",\"razaoSocial\":\"Padaria Pao Quente\",\"saldo\":1500.00,"
                    + "\"lancamentos\":[{\"tipo\":\"DEPOSITO\",\"descricao\":\"Deposito inicial\",\"valor\":1000.00},"
                    + "{\"tipo\":\"DEPOSITO\",\"descricao\":\"Deposito em conta\",\"valor\":500.00}]}";
            assertEquals(esperado, servico.gerarExtrato(CNPJ_A, "JSON"));
        }

        @Test
        void jsonEscapaAspasDaRazaoSocial() {
            servico.abrirConta(CNPJ_A, "Loja \"Do Ze\"", TipoEmpresa.ME, "a@b.com", "6199", BigDecimal.ZERO);
            assertTrue(servico.gerarExtrato(CNPJ_A, "JSON").contains("\"razaoSocial\":\"Loja \\\"Do Ze\\\"\""));
        }

        @Test
        void formatoNaoDependeDeCaixaAltaOuBaixa() {
            montarMovimentacao();
            assertEquals(servico.gerarExtrato(CNPJ_A, "CSV"), servico.gerarExtrato(CNPJ_A, "csv"));
        }

        @Test
        void rejeitaFormatoInvalido() {
            abrir(CNPJ_A, TipoEmpresa.ME, "0");
            assertThrows(OperacaoInvalidaException.class, () -> servico.gerarExtrato(CNPJ_A, "XML"));
            assertThrows(OperacaoInvalidaException.class, () -> servico.gerarExtrato(CNPJ_A, null));
        }

        @Test
        void extratoSemMovimentosTemApenasCabecalho() {
            abrir(CNPJ_A, TipoEmpresa.ME, "0");
            assertEquals("tipo;descricao;valor\n", servico.gerarExtrato(CNPJ_A, "CSV"));
        }
    }

    // =====================================================================
    @Nested
    class TarifaMensal {

        @ParameterizedTest
        @CsvSource({"MEI,0.00", "ME,29.90", "EPP,49.90", "LTDA,79.90", "SA,149.90"})
        void cobraTarifaPorTipo(TipoEmpresa tipo, String tarifa) {
            abrir(CNPJ_A, tipo, "10000");
            BigDecimal cobrada = servico.cobrarTarifaMensal(CNPJ_A);
            assertValor(tarifa, cobrada);
            assertSaldo(CNPJ_A, new BigDecimal("10000").subtract(new BigDecimal(tarifa)).toPlainString());
        }

        @Test
        void meiNaoGeraLancamentoDeTarifa() {
            abrir(CNPJ_A, TipoEmpresa.MEI, "100");
            servico.cobrarTarifaMensal(CNPJ_A);
            org.junit.jupiter.api.Assertions.assertFalse(servico.gerarExtrato(CNPJ_A, "CSV").contains("TARIFA"));
        }

        @Test
        void tarifaPodeDeixarSaldoNegativo() {
            abrir(CNPJ_A, TipoEmpresa.ME, "0");
            servico.cobrarTarifaMensal(CNPJ_A);
            assertSaldo(CNPJ_A, "-29.90");
        }
    }

    // =====================================================================
    @Nested
    class CaixaEletronico {

        CaixaEletronicoPJ caixa;

        @BeforeEach
        void prepararCaixa() {
            caixa = new CaixaEletronicoPJ(servico);
            abrir(CNPJ_A, TipoEmpresa.ME, "1000");
        }

        @Test
        void sacaValorMultiploDeDez() {
            caixa.sacar(CNPJ_A, new BigDecimal("200"));
            assertValor("798.50", caixa.consultarSaldo(CNPJ_A));
        }

        @Test
        void rejeitaValorNaoMultiploDeDez() {
            assertThrows(OperacaoInvalidaException.class, () -> caixa.sacar(CNPJ_A, new BigDecimal("105")));
            assertSaldo(CNPJ_A, "1000.00");
        }

        @Test
        void rejeitaSaqueAcimaDoLimiteDoCaixa() {
            assertThrows(OperacaoInvalidaException.class, () -> caixa.sacar(CNPJ_A, new BigDecimal("5010")));
            assertSaldo(CNPJ_A, "1000.00");
        }

        @Test
        void aceitaSaqueNoLimiteDoCaixa() {
            caixa.sacar(CNPJ_A, new BigDecimal("5000"));
            assertSaldo(CNPJ_A, "-4001.50");
        }

        @Test
        void depositaEmEnvelope() {
            caixa.depositarEnvelope(CNPJ_A, new BigDecimal("500"));
            assertSaldo(CNPJ_A, "1500.00");
        }

        @Test
        void rejeitaDepositoEmEnvelopeAcimaDoLimite() {
            assertThrows(OperacaoInvalidaException.class, () -> caixa.depositarEnvelope(CNPJ_A, new BigDecimal("10000.01")));
            assertSaldo(CNPJ_A, "1000.00");
        }

        @Test
        void saqueNoCaixaRespeitaSaldoDaConta() {
            abrir(CNPJ_B, TipoEmpresa.MEI, "100");
            assertThrows(SaldoInsuficienteException.class, () -> caixa.sacar(CNPJ_B, new BigDecimal("110")));
        }
    }
}
