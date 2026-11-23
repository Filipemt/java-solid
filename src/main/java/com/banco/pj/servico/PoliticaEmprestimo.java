package com.banco.pj.servico;

import com.banco.pj.dominio.CondicoesEmprestimo;
import com.banco.pj.dominio.enums.TipoEmpresa;

public interface PoliticaEmprestimo {
    CondicoesEmprestimo determinarCondicoesEmprestimo(TipoEmpresa tipoEmpresa);
}
