import easyaccept.EasyAccept;

public class Main {
    public static void main(String[] args) {
        String facade = "br.ufal.ic.p2.wepayu.Facade";
        String[] testes = args.length == 0
                ? new String[]{"tests/us1.txt", "tests/us2.txt", "tests/us3.txt",
                               "tests/us4.txt", "tests/us5.txt", "tests/us6.txt"}
                : args;
        for (String teste : testes) EasyAccept.main(new String[]{facade, teste});
    }
}
