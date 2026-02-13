package org.example.controllers.Culture;

import javafx.animation.PauseTransition;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.Duration;
import org.example.entities.Culture;
import org.example.services.CultureService;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

public class CultureController {

    @FXML private TextField txtNom, txtType, txtSuperficie, txtLocalisation;
    @FXML private Label lblMessage, lblImage;
    @FXML private ImageView imageView;
    @FXML private Button btnAction; // 🔥 bouton unique (Ajouter / Modifier)

    private File imageFile;
    private String imagePath;
    private Culture cultureEnCours = null;

    private final CultureService service = new CultureService();

    // ================= IMAGE =================
    @FXML
    private void choisirImage() {
        FileChooser chooser = new FileChooser();
        chooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Images", "*.png","*.jpg","*.jpeg")
        );
        File file = chooser.showOpenDialog(null);

        if(file != null){
            imageFile = file;
            imageView.setImage(new Image(file.toURI().toString()));
            lblImage.setText(file.getName());
        }
    }

    // ================= MODE MODIFICATION =================
    public void remplirFormulaire(Culture c){
        if(c == null) return;

        cultureEnCours = c;
        btnAction.setText("✏️ Modifier");

        txtNom.setText(c.getNom());
        txtType.setText(c.getType());
        txtSuperficie.setText(String.valueOf(c.getSuperficie()));
        txtLocalisation.setText(c.getLocalisation());

        imagePath = c.getImage();
        if(imagePath != null){
            File f = new File(imagePath);
            if(f.exists()){
                imageView.setImage(new Image(f.toURI().toString()));
                lblImage.setText(f.getName());
            }
        }
    }

    // ================= ACTION PRINCIPALE =================
    @FXML
    private void handleAction() {

        try {
            Culture c = getCultureFromForm();

            if(cultureEnCours == null){
                service.ajouter(c);
                afficherMessage("✅ Culture ajoutée !");
            } else {
                c.setIdCulture(cultureEnCours.getIdCulture());
                service.modifier(c);
                afficherMessage("✏️ Culture modifiée !");
            }

            fermerFenetre();

        } catch (Exception e) {
            lblMessage.setText("❌ " + e.getMessage());
        }
    }

    private Culture getCultureFromForm() {

        if(txtNom.getText().isEmpty() || txtSuperficie.getText().isEmpty())
            throw new IllegalArgumentException("Nom et Superficie obligatoires");

        String img = imageFile != null ? copierImage(imageFile) : imagePath;

        return new Culture(
                0,
                txtNom.getText(),
                txtType.getText(),
                Double.parseDouble(txtSuperficie.getText()),
                txtLocalisation.getText(),
                img
        );
    }

    private String copierImage(File file){
        try {
            File dir = new File("images");
            if(!dir.exists()) dir.mkdirs();

            String nom = System.currentTimeMillis()+"_"+file.getName();
            File dest = new File(dir, nom);

            Files.copy(file.toPath(), dest.toPath(), StandardCopyOption.REPLACE_EXISTING);
            return dest.getPath();

        } catch(Exception e){
            return null;
        }
    }

    private void afficherMessage(String msg){
        lblMessage.setText(msg);
        PauseTransition pause = new PauseTransition(Duration.seconds(2));
        pause.setOnFinished(e -> lblMessage.setText(""));
        pause.play();
    }

    @FXML
    private void fermerFenetre(){
        ((Stage) txtNom.getScene().getWindow()).close();
    }
}
