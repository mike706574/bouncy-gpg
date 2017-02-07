package name.neuhalfen.projects.crypto.bouncycastle.openpgp.example;

import name.neuhalfen.projects.crypto.bouncycastle.openpgp.decrypting.DecryptionStreamFactory;
import name.neuhalfen.projects.crypto.bouncycastle.openpgp.encrypting.EncryptWithOpenPGP;
import name.neuhalfen.projects.crypto.bouncycastle.openpgp.encrypting.EncryptionConfig;
import name.neuhalfen.projects.crypto.bouncycastle.openpgp.keys.KeyringConfig;
import name.neuhalfen.projects.crypto.bouncycastle.openpgp.keys.KeyringConfigCallbacks;
import name.neuhalfen.projects.crypto.bouncycastle.openpgp.keys.KeyringConfigs;
import name.neuhalfen.projects.crypto.bouncycastle.openpgp.reencryption.FSZipEntityStrategy;
import name.neuhalfen.projects.crypto.bouncycastle.openpgp.reencryption.ReencryptExplodedZipMultithreaded;
import name.neuhalfen.projects.crypto.bouncycastle.openpgp.reencryption.ZipEntityStrategy;
import name.neuhalfen.projects.crypto.bouncycastle.openpgp.validation.SignatureValidationStrategies;
import org.bouncycastle.bcpg.SymmetricKeyAlgorithmTags;
import org.bouncycastle.crypto.tls.HashAlgorithm;
import org.bouncycastle.jce.provider.BouncyCastleProvider;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.security.Security;

/**
 * Multithreaded implementation. Not tested that much.
 */
public class MainExplodedMultithreaded {
    static void installBCProvider() {
        if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
            Security.addProvider(new BouncyCastleProvider());
        }
    }


    public static void main(String[] args) {
        if (args.length != 6) {
            System.err.format("Usage %s  recipient pubKeyRing secKeyRing secKeyRingPassword sourceFile.zip.gpg destPath\n", "java -jar xxx.jar");
            System.exit(-1);
        } else {
            final String recipient = args[0];
            final File pubKeyRing = new File(args[1]);
            final File secKeyRing = new File(args[2]);
            final String secKeyRingPassword = args[3];
            final File sourceFile = new File(args[4]);
            final File destRootDir = new File(args[5]);

            try {
                installBCProvider();

                final KeyringConfig keyringConfig = KeyringConfigs.withKeyRingsFromFiles(pubKeyRing,
                        secKeyRing, KeyringConfigCallbacks.withPassword(secKeyRingPassword));

                final EncryptionConfig encryptionConfig = new EncryptionConfig(
                        recipient,
                        recipient,
                        HashAlgorithm.sha1,
                        SymmetricKeyAlgorithmTags.AES_128, keyringConfig);


                final DecryptionStreamFactory decryptionStreamFactory = new DecryptionStreamFactory(keyringConfig, SignatureValidationStrategies.requireAnySignature());

                long startTime = System.currentTimeMillis();

                final EncryptWithOpenPGP encryptWithOpenPGP = new EncryptWithOpenPGP(encryptionConfig);
                final ZipEntityStrategy zipEntityStrategy = new FSZipEntityStrategy(destRootDir);
                final ReencryptExplodedZipMultithreaded reencryptExplodedZip = new ReencryptExplodedZipMultithreaded();


                try (
                        final InputStream encryptedStream = new FileInputStream(sourceFile);
                        final InputStream decryptedStream = decryptionStreamFactory.wrapWithDecryptAndVerify(encryptedStream)
                ) {
                    reencryptExplodedZip.explodeAndReencrypt(decryptedStream, zipEntityStrategy, encryptWithOpenPGP);
                }
                long endTime = System.currentTimeMillis();

                System.out.format("Re-Encryption took %.2f s\n", ((double) endTime - startTime) / 1000);
            } catch (Exception e) {
                System.err.format("ERROR: %s", e.getMessage());
                e.printStackTrace();
            }
        }
    }
}
