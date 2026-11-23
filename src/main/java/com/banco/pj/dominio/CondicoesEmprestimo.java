package com.banco.pj.dominio;

import java.math.BigDecimal;

public record CondicoesEmprestimo(BigDecimal valorMaximo,
                                  BigDecimal taxa,
                                  int bonusTipo,
                                  int maxEmprestimos) {}
