import java.io.*;
import java.nio.file.*;
import java.security.*;
import java.security.KeyStore;
import java.util.Base64;

public class UpdateSigner {

    public static void main(String[] args) throws Exception {

        if (args.length != 4) {
            System.err.println(
                "Usage: UpdateSigner <keystore> <alias> <data-file> <output-file>"
            );
            System.exit(2);
        }

        String passwordText = readPassword();
        if (passwordText == null || passwordText.isEmpty()) {
            throw new Exception("Password was not supplied");
        }

        char[] password = passwordText.toCharArray();

        try {
            KeyStore ks = KeyStore.getInstance("PKCS12");

            try (InputStream in = new FileInputStream(args[0])) {
                ks.load(in, password);
            }

            Key key = ks.getKey(args[1], password);

            if (!(key instanceof PrivateKey)) {
                throw new Exception("Alias does not contain a private key");
            }

            byte[] data = Files.readAllBytes(
                Paths.get(args[2])
            );

            Signature signer =
                Signature.getInstance("SHA256withRSA");

            signer.initSign((PrivateKey) key);
            signer.update(data);

            String signature =
                Base64.getEncoder().encodeToString(
                    signer.sign()
                );

            Files.writeString(
                Paths.get(args[3]),
                signature,
                java.nio.charset.StandardCharsets.US_ASCII
            );

        } finally {
            java.util.Arrays.fill(password, '\0');
        }
    }

    private static String readPassword() throws IOException {
        BufferedReader reader =
            new BufferedReader(
                new InputStreamReader(System.in)
            );

        String line = reader.readLine();

        if (line == null) {
            return null;
        }

        return line.trim();
    }
}
