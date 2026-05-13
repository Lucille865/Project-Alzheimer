import javax.sound.sampled.*;

/**
 * Génère un son d'alerte programmatiquement (pas besoin de fichier audio).
 * Deux sons : un bip doux pour la validation, un bip insistant pour le rappel.
 */
public class SonRappel {

    /** Bip court de confirmation (validation d'une tâche). */
    public static void bipValidation() {
        jouer(880, 180, 0.4f);
    }

    /**
     * Séquence de 3 bips insistants pour le rappel 30 min.
     * Lancé dans un thread séparé pour ne pas bloquer l'UI.
     */
    public static void bipRappel() {
        new Thread(() -> {
            for (int i = 0; i < 3; i++) {
                jouer(520, 400, 0.7f);
                try { Thread.sleep(200); } catch (InterruptedException ignored) {}
            }
        }, "thread-son-rappel").start();
    }

    /**
     * @param frequenceHz  hauteur du son (Hz)
     * @param dureeMilis   durée en millisecondes
     * @param volume       amplitude 0.0 → 1.0
     */
    private static void jouer(int frequenceHz, int dureeMilis, float volume) {
        try {
            float  sampleRate  = 44100f;
            int    nbSamples   = (int) (sampleRate * dureeMilis / 1000);
            byte[] buffer      = new byte[nbSamples * 2]; // 16 bits = 2 octets/sample

            for (int i = 0; i < nbSamples; i++) {
                // Onde sinusoïdale + enveloppe fade-out pour adoucir la fin
                double angle     = 2.0 * Math.PI * i * frequenceHz / sampleRate;
                double envelope  = 1.0 - (double) i / nbSamples; // fade linéaire
                short  sample    = (short) (Math.sin(angle) * volume * envelope * Short.MAX_VALUE);
                buffer[2 * i]     = (byte) (sample & 0xff);
                buffer[2 * i + 1] = (byte) ((sample >> 8) & 0xff);
            }

            AudioFormat format = new AudioFormat(sampleRate, 16, 1, true, false);
            try (SourceDataLine line = AudioSystem.getSourceDataLine(format)) {
                line.open(format, buffer.length);
                line.start();
                line.write(buffer, 0, buffer.length);
                line.drain();
            }
        } catch (LineUnavailableException e) {
            System.err.println("[Son] Ligne audio indisponible : " + e.getMessage());
        }
    }
}
