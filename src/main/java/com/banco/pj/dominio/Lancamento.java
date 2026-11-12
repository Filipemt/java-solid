package com.banco.pj.dominio;

import java.math.BigDecimal;

public record Lancamento(String tipo, String descricao, BigDecimal valor) {
}
