package br.ufpa.tdd;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Conta digital da AçaíPay: controla o saldo e mantém o extrato (ledger)
 * das operações na ordem em que ocorreram.
 *
 * <p>Toda operação inválida lança exceção <b>antes</b> de alterar qualquer estado,
 * portanto saldo e extrato nunca ficam parcialmente atualizados.
 */
public class Conta {

    private static final String TIPO_DEPOSITO = "DEPOSITO";
    private static final String TIPO_SAQUE = "SAQUE";

    private int saldo;
    private final List<Entrada> entradas = new ArrayList<>();

    public int getSaldo() {
        return saldo;
    }

    /**
     * Visão somente leitura do extrato. Quem usa a classe não consegue
     * adicionar, editar ou remover lançamentos (UnsupportedOperationException).
     */
    public List<Entrada> getEntradas() {
        return Collections.unmodifiableList(entradas);
    }

    public void deposito(int quantia) {
        validarQuantiaPositiva(quantia);
        saldo += quantia;
        registrar(TIPO_DEPOSITO, quantia);
    }

    public void saque(int quantia) {
        validarQuantiaPositiva(quantia);
        validarSaldoSuficiente(quantia);
        saldo -= quantia;
        registrar(TIPO_SAQUE, quantia);
    }

    /**
     * Transfere {@code quantia} desta conta para {@code outra}: a origem registra
     * um SAQUE e o destino registra um DEPOSITO.
     */
    public void transferenciaPara(Conta outra, int quantia) {
        Objects.requireNonNull(outra, "A conta de destino é obrigatória");
        // saque() valida quantia e saldo antes de mexer em qualquer estado; se falhar,
        // nenhuma das duas contas foi alterada. Se passar, o depósito não pode falhar
        // porque a quantia já se provou positiva.
        saque(quantia);
        outra.deposito(quantia);
    }

    private void validarQuantiaPositiva(int quantia) {
        if (quantia <= 0) {
            throw new IllegalArgumentException("A quantia deve ser positiva, mas foi: " + quantia);
        }
    }

    private void validarSaldoSuficiente(int quantia) {
        if (quantia > saldo) {
            throw new ExcecaoConta(
                    "Saldo insuficiente: saldo=" + saldo + ", quantia solicitada=" + quantia);
        }
    }

    private void registrar(String tipo, int quantia) {
        entradas.add(new Entrada(tipo, quantia, saldo));
    }

    /**
     * Lançamento do extrato. Imutável (record), então uma vez registrado não pode ser editado.
     *
     * @param tipo      "DEPOSITO" ou "SAQUE"
     * @param valor     quantia da operação (sempre positiva)
     * @param saldoApos saldo da conta imediatamente após a operação
     */
    public record Entrada(String tipo, int valor, int saldoApos) {
    }

    /** Violação de regra de negócio da conta (ex.: saldo insuficiente). */
    public static class ExcecaoConta extends RuntimeException {
        private static final long serialVersionUID = 1L;

        public ExcecaoConta(String mensagem) {
            super(mensagem);
        }
    }
}
