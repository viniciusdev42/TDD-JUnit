package br.ufpa.tdd;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import br.ufpa.tdd.Conta.Entrada;
import br.ufpa.tdd.Conta.ExcecaoConta;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class ContaTest {

    private Conta conta;

    @BeforeEach
    void criarConta() {
        conta = new Conta();
    }

    // ------------------------------------------------------------------
    // 1) Estado inicial
    // ------------------------------------------------------------------

    @Test
    @DisplayName("saldo inicial é 0")
    void saldoInicialEhZero() {
        assertEquals(0, conta.getSaldo());
    }

    @Test
    @DisplayName("extrato inicial é vazio")
    void extratoInicialEhVazio() {
        assertTrue(conta.getEntradas().isEmpty());
    }

    // ------------------------------------------------------------------
    // 2) Depósito
    // ------------------------------------------------------------------

    @Test
    @DisplayName("depósito aumenta o saldo")
    void depositoAumentaSaldo() {
        conta.deposito(100);

        assertEquals(100, conta.getSaldo());
    }

    @Test
    @DisplayName("depósitos sucessivos acumulam no saldo")
    void depositosSucessivosAcumulamSaldo() {
        conta.deposito(100);
        conta.deposito(50);

        assertEquals(150, conta.getSaldo());
    }

    @Test
    @DisplayName("depósito registra lançamento DEPOSITO com valor e saldo pós-operação")
    void depositoRegistraEntradaNoExtrato() {
        conta.deposito(100);

        assertEquals(List.of(new Entrada("DEPOSITO", 100, 100)), conta.getEntradas());
    }

    // ------------------------------------------------------------------
    // 3) Saque
    // ------------------------------------------------------------------

    @Test
    @DisplayName("saque diminui o saldo")
    void saqueDiminuiSaldo() {
        conta.deposito(100);

        conta.saque(30);

        assertEquals(70, conta.getSaldo());
    }

    @Test
    @DisplayName("saque registra lançamento SAQUE com valor positivo e saldo pós-operação")
    void saqueRegistraEntradaNoExtrato() {
        conta.deposito(100);

        conta.saque(30);

        Entrada ultima = conta.getEntradas().get(1);
        assertEquals(new Entrada("SAQUE", 30, 70), ultima);
    }

    @Test
    @DisplayName("saque do valor exato do saldo é permitido e zera a conta")
    void saqueDoSaldoExatoZeraConta() {
        conta.deposito(100);

        conta.saque(100);

        assertEquals(0, conta.getSaldo());
    }

    // ------------------------------------------------------------------
    // 4) Extrato (ledger)
    // ------------------------------------------------------------------

    @Test
    @DisplayName("extrato mantém as operações na ordem em que ocorreram")
    void extratoMantemOrdemDasOperacoes() {
        conta.deposito(100);
        conta.saque(30);
        conta.deposito(50);

        assertEquals(
                List.of(
                        new Entrada("DEPOSITO", 100, 100),
                        new Entrada("SAQUE", 30, 70),
                        new Entrada("DEPOSITO", 50, 120)),
                conta.getEntradas());
    }

    @Test
    @DisplayName("extrato não aceita novos lançamentos feitos por fora")
    void extratoNaoAceitaInsercaoExterna() {
        List<Entrada> extrato = conta.getEntradas();

        assertThrows(UnsupportedOperationException.class,
                () -> extrato.add(new Entrada("DEPOSITO", 999, 999)));
        assertTrue(conta.getEntradas().isEmpty());
    }

    @Test
    @DisplayName("extrato não permite apagar lançamentos por fora")
    void extratoNaoPermiteRemocaoExterna() {
        conta.deposito(100);
        List<Entrada> extrato = conta.getEntradas();

        assertThrows(UnsupportedOperationException.class, () -> extrato.remove(0));
        assertThrows(UnsupportedOperationException.class, extrato::clear);
        assertEquals(1, conta.getEntradas().size());
    }

    // ------------------------------------------------------------------
    // 5) Valores inválidos
    // ------------------------------------------------------------------

    @ParameterizedTest(name = "depósito de {0} lança IllegalArgumentException")
    @ValueSource(ints = {0, -1, -100})
    void depositoComQuantiaInvalidaLancaExcecao(int quantiaInvalida) {
        assertThrows(IllegalArgumentException.class, () -> conta.deposito(quantiaInvalida));
    }

    @ParameterizedTest(name = "depósito de {0} não altera saldo nem extrato")
    @ValueSource(ints = {0, -1, -100})
    void depositoInvalidoNaoTemEfeitoColateral(int quantiaInvalida) {
        assertThrows(IllegalArgumentException.class, () -> conta.deposito(quantiaInvalida));

        assertEquals(0, conta.getSaldo());
        assertTrue(conta.getEntradas().isEmpty());
    }

    @ParameterizedTest(name = "saque de {0} lança IllegalArgumentException")
    @ValueSource(ints = {0, -1, -100})
    void saqueComQuantiaInvalidaLancaExcecao(int quantiaInvalida) {
        conta.deposito(100);

        assertThrows(IllegalArgumentException.class, () -> conta.saque(quantiaInvalida));
    }

    @ParameterizedTest(name = "saque de {0} não altera saldo nem extrato")
    @ValueSource(ints = {0, -1, -100})
    void saqueInvalidoNaoTemEfeitoColateral(int quantiaInvalida) {
        conta.deposito(100);
        List<Entrada> extratoAntes = List.copyOf(conta.getEntradas());

        assertThrows(IllegalArgumentException.class, () -> conta.saque(quantiaInvalida));

        assertEquals(100, conta.getSaldo());
        assertEquals(extratoAntes, conta.getEntradas());
    }

    // ------------------------------------------------------------------
    // 6) Saldo insuficiente
    // ------------------------------------------------------------------

    @Test
    @DisplayName("saque maior que o saldo lança ExcecaoConta")
    void saqueMaiorQueSaldoLancaExcecao() {
        conta.deposito(50);

        assertThrows(ExcecaoConta.class, () -> conta.saque(51));
    }

    @Test
    @DisplayName("saque maior que o saldo não altera saldo nem extrato")
    void saqueMaiorQueSaldoNaoTemEfeitoColateral() {
        conta.deposito(50);
        List<Entrada> extratoAntes = List.copyOf(conta.getEntradas());

        assertThrows(ExcecaoConta.class, () -> conta.saque(51));

        assertEquals(50, conta.getSaldo());
        assertEquals(extratoAntes, conta.getEntradas());
    }

    // ------------------------------------------------------------------
    // BÔNUS) Transferência
    // ------------------------------------------------------------------

    @Test
    @DisplayName("transferência diminui o saldo da origem e aumenta o do destino")
    void transferenciaMovimentaSaldoDasDuasContas() {
        Conta destino = new Conta();
        conta.deposito(100);

        conta.transferenciaPara(destino, 40);

        assertEquals(60, conta.getSaldo());
        assertEquals(40, destino.getSaldo());
    }

    @Test
    @DisplayName("transferência registra SAQUE na origem")
    void transferenciaRegistraSaqueNaOrigem() {
        Conta destino = new Conta();
        conta.deposito(100);

        conta.transferenciaPara(destino, 40);

        assertEquals(new Entrada("SAQUE", 40, 60), conta.getEntradas().get(1));
    }

    @Test
    @DisplayName("transferência registra DEPOSITO no destino")
    void transferenciaRegistraDepositoNoDestino() {
        Conta destino = new Conta();
        conta.deposito(100);

        conta.transferenciaPara(destino, 40);

        assertEquals(List.of(new Entrada("DEPOSITO", 40, 40)), destino.getEntradas());
    }

    @ParameterizedTest(name = "transferência de {0} lança exceção e não altera nenhuma conta")
    @ValueSource(ints = {0, -1, -100})
    void transferenciaComQuantiaInvalidaNaoTemEfeitoColateral(int quantiaInvalida) {
        Conta destino = new Conta();
        conta.deposito(100);
        List<Entrada> extratoOrigemAntes = List.copyOf(conta.getEntradas());

        assertThrows(IllegalArgumentException.class,
                () -> conta.transferenciaPara(destino, quantiaInvalida));

        assertEquals(100, conta.getSaldo());
        assertEquals(extratoOrigemAntes, conta.getEntradas());
        assertEquals(0, destino.getSaldo());
        assertTrue(destino.getEntradas().isEmpty());
    }

    @Test
    @DisplayName("transferência com saldo insuficiente lança exceção e não altera nenhuma conta")
    void transferenciaComSaldoInsuficienteNaoTemEfeitoColateral() {
        Conta destino = new Conta();
        conta.deposito(50);
        List<Entrada> extratoOrigemAntes = List.copyOf(conta.getEntradas());

        assertThrows(ExcecaoConta.class, () -> conta.transferenciaPara(destino, 51));

        assertEquals(50, conta.getSaldo());
        assertEquals(extratoOrigemAntes, conta.getEntradas());
        assertEquals(0, destino.getSaldo());
        assertTrue(destino.getEntradas().isEmpty());
    }

    @Test
    @DisplayName("transferência para conta nula lança exceção e não altera a origem")
    void transferenciaParaContaNulaNaoAlteraOrigem() {
        conta.deposito(100);

        assertThrows(NullPointerException.class, () -> conta.transferenciaPara(null, 40));

        assertEquals(100, conta.getSaldo());
        assertEquals(1, conta.getEntradas().size());
    }
}
