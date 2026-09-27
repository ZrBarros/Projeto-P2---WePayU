# WePayU

Até aqui o sistema permite cadastrar, remover e alterar empregados. Também é possível lançar cartões de ponto, vendas e taxas de serviço. Os três tipos de empregado pedidos no trabalho já estão disponíveis: horista, assalariado e comissionado.

## Organização do código

A classe `Facade` recebe os comandos do EasyAccept. A classe `SistemaFolha` guarda os empregados cadastrados e gera os identificadores. Os dados de cada funcionário e seus lançamentos ficam em `Empregado`.

Os valores em dinheiro foram tratados com `BigDecimal`, para não ter os problemas de arredondamento de `double`. Para as datas foi usado `LocalDate`.

## Como rodar o código

Dentro da pasta do projeto, execute:

```sh
mkdir -p build/classes
javac -encoding UTF-8 -cp lib/easyaccept.jar -d build/classes $(find src -name '*.java')
java -cp build/classes:lib/easyaccept.jar Main
```

Os arquivos que terminam em `_1` são os testes de persistência. Eles devem ser executados logo depois do teste principal da mesma história.
