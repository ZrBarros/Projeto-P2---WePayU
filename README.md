# WePayU

Sistema de folha de pagamento desenvolvido em Java para a disciplina de Programação 2. O projeto implementa as histórias 1 a 8 do primeiro milestone.

## Funcionalidades

O sistema permite cadastrar, remover e alterar empregados horistas, assalariados e comissionados. Também é possível lançar cartões de ponto, resultados de vendas e taxas de serviço do sindicato.

Ao rodar a folha de pagamento, o programa verifica quais empregados recebem na data informada e calcula salário, horas extras, comissões e descontos. O pagamento pode ser feito em mãos, pelos correios ou por depósito bancário.

As operações das histórias 1 a 7 podem ser desfeitas e refeitas com `undo` e `redo`. Os dados também são salvos em arquivo quando o sistema é encerrado.

## Organização do código

A `Facade` é o ponto de entrada usado pelo EasyAccept. `SistemaFolha` cuida do estado do programa e `Empregado` reúne os dados e lançamentos de cada funcionário. As regras de cálculo e a montagem do relatório ficam em `FolhaPagamento`, separadas do restante do cadastro.

Os valores monetários usam `BigDecimal` e as datas usam `LocalDate`. A persistência é feita no arquivo `wepayu-state.bin`.

## Como testar

Dentro da pasta do projeto, execute:

```sh
mkdir -p build/classes
javac -encoding UTF-8 -cp lib/easyaccept.jar -d build/classes $(find src -name '*.java')
java -cp build/classes:lib/easyaccept.jar Main
```

Os testes de persistência devem ser executados logo depois do teste principal correspondente.

As histórias 9 e 10, relacionadas às agendas personalizadas de pagamento, não fazem parte desta versão.
