package com.banco.pj.servico;

import com.banco.pj.dominio.ContaPJ;

public interface GeradorExtrato {
    String gerarExtrato(ContaPJ conta);
}
