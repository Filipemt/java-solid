package com.banco.pj.dominio;

import java.math.BigDecimal;

public class ContaMEI extends ContaPJ {

    public ContaMEI(String cnpj) {
        super(cnpj);
    }

    @Override
    public void setLimiteChequeEspecial(BigDecimal limiteChequeEspecial) {
        throw new UnsupportedOperationException("Conta MEI nao possui cheque especial");
    }
}
