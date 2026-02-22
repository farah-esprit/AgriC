package org.example.utils;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;

import java.util.HashMap;
import java.util.Map;

public class QRCodeGenerator {

    public static javafx.scene.image.WritableImage genererFX(String contenu, int largeur, int hauteur)
            throws WriterException {

        Map<EncodeHintType, Object> hints = new HashMap<>();
        hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.H);
        hints.put(EncodeHintType.MARGIN, 2);
        hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");

        QRCodeWriter writer = new QRCodeWriter();
        BitMatrix bitMatrix = writer.encode(contenu, BarcodeFormat.QR_CODE, largeur, hauteur, hints);

        javafx.scene.image.WritableImage image = new javafx.scene.image.WritableImage(largeur, hauteur);
        javafx.scene.image.PixelWriter pw = image.getPixelWriter();

        javafx.scene.paint.Color couleurQR = javafx.scene.paint.Color.web("#4a7c3a");
        javafx.scene.paint.Color blanc = javafx.scene.paint.Color.WHITE;

        for (int x = 0; x < largeur; x++) {
            for (int y = 0; y < hauteur; y++) {
                pw.setColor(x, y, bitMatrix.get(x, y) ? couleurQR : blanc);
            }
        }
        return image;
    }

    public static javafx.scene.image.WritableImage genererAgriConnect(String contenu)
            throws WriterException {
        return genererFX(contenu, 300, 300);
    }

    public static String formatProduit(String nom, String categorie, double prix, String description) {
        return String.format(
                "AgriConnect - Produit\n" +
                        "Nom: %s\n" +
                        "Categorie: %s\n" +
                        "Prix: %.2f DT\n" +
                        "Description: %s",
                nom, categorie, prix, description
        );
    }

    public static String formatCommande(int idCommande, String produit, int quantite, String statut, String date) {
        return String.format(
                "AgriConnect - Commande\n" +
                        "ID: #%d\n" +
                        "Produit: %s\n" +
                        "Quantite: %d\n" +
                        "Statut: %s\n" +
                        "Date: %s",
                idCommande, produit, quantite, statut, date
        );
    }
}