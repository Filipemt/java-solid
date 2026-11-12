# Banco PJ

Serviço de conta para **pessoa jurídica** de um banco (Java 17 + Maven + JUnit 5).

## Funcionalidades

- Abertura e encerramento de conta (MEI, ME, EPP, LTDA, SA)
- Depósito, saque, transferência (PIX, TED, interna) e pagamento de boleto
- Empréstimo, cheque especial e tarifa mensal
- Extrato em TXT, CSV e JSON
- Notificações (e-mail/SMS) e trilha de auditoria
- Caixa eletrônico PJ

## Como rodar

```bash
mvn test                                   # roda os 109 testes
mvn compile && java -cp target/classes com.banco.pj.App   # demo
```

## Regra do jogo da refatoração

Os testes são o contrato de comportamento: **refatore o código de produção, não os testes**.

- Os testes usam somente a API pública de `ServicoContaPJ` e `CaixaEletronicoPJ`.
- O único ponto de teste que pode precisar de ajuste (ex.: se você introduzir injeção de dependência)
  é o método `criarServico()` em `ServicoContaPJTest`.
- Se um teste quebrar após a refatoração, o comportamento mudou.
