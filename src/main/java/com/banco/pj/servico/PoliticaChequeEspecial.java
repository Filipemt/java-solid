package com.banco.pj.servico;

import com.banco.pj.dominio.CondicoesLimiteChequeEspecial;
import com.banco.pj.dominio.enums.TipoEmpresa;

public interface PoliticaChequeEspecial {
    CondicoesLimiteChequeEspecial determinarCondicoesLimiteChequeEspecial(TipoEmpresa tipoEmpresa);
}
