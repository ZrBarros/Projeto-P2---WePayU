# WePayU

Esta é a segunda versão do trabalho. Ela continua o que foi feito na primeira etapa e completa as histórias 1 a 8 do primeiro milestone. As histórias 9 e 10 não foram incluídas.

## O que foi acrescentado

Nesta etapa foi implementado o cálculo da folha de pagamento. O sistema verifica quais empregados recebem na data informada e calcula salário, horas extras, comissões e descontos do sindicato.

Os pagamentos podem ser feitos em mãos, por cheque enviado pelos correios ou por depósito bancário. Ao rodar a folha, o programa também gera o relatório no formato pedido pelos testes.

Outra mudança desta versão foi a inclusão de `undo` e `redo`. Antes de uma operação que altera o sistema, uma cópia do estado é guardada. Com isso é possível desfazer uma operação e refazê-la depois.

## Organização do código

A `Facade` continua sendo o ponto de entrada usado pelo EasyAccept. `SistemaFolha` cuida do estado do programa e `Empregado` reúne os dados e lançamentos de cada funcionário. As regras de cálculo e a montagem do relatório ficaram em `FolhaPagamento`, separadas do restante do cadastro.

Os valores monetários usam `BigDecimal` e as datas usam `LocalDate`. A persistência é feita no arquivo `wepayu-state.bin`.

## Como testar

Dentro da pasta do projeto, execute:

```sh
mkdir -p build/classes
javac -encoding UTF-8 -cp lib/easyaccept.jar -d build/classes $(find src -name '*.java')
java -cp build/classes:lib/easyaccept.jar Main
```

Os testes de persistência devem ser executados logo depois do teste principal correspondente.
