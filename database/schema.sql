-- Création de la base de données
CREATE DATABASE IF NOT EXISTS event_management CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE event_management;

-- Table UTILISATEUR
CREATE TABLE IF NOT EXISTS utilisateur (
    id INT AUTO_INCREMENT PRIMARY KEY,
    nom VARCHAR(100) NOT NULL,
    email VARCHAR(150) NOT NULL UNIQUE,
    mot_de_passe VARCHAR(255) NOT NULL,
    role ENUM('USER', 'ADMIN') DEFAULT 'USER',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Table EVENEMENT
CREATE TABLE IF NOT EXISTS evenement (
    id_evenement INT AUTO_INCREMENT PRIMARY KEY,
    titre VARCHAR(200) NOT NULL,
    description TEXT,
    date_debut DATE NOT NULL,
    date_fin DATE NOT NULL,
    lieu VARCHAR(200) NOT NULL,
    capacite_max INT NOT NULL,
    organisateur_id INT NOT NULL,
    statut ENUM('APPROUVE', 'EN_ATTENTE', 'REJETE') DEFAULT 'EN_ATTENTE',
    raison_rejet VARCHAR(500),
    image_url VARCHAR(500),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (organisateur_id) REFERENCES utilisateur(id) ON DELETE CASCADE,
    INDEX idx_organisateur (organisateur_id),
    INDEX idx_date_debut (date_debut),
    INDEX idx_statut (statut)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Table RECLAMATION
CREATE TABLE IF NOT EXISTS reclamation (
    id_reclamation INT AUTO_INCREMENT PRIMARY KEY,
    objet VARCHAR(200) NOT NULL,
    description TEXT NOT NULL,
    date_creation DATE NOT NULL,
    statut ENUM('EN_ATTENTE', 'EN_COURS', 'TRAITEE', 'CLOTUREE') DEFAULT 'EN_ATTENTE',
    priorite VARCHAR(50) DEFAULT 'MOYENNE',
    type VARCHAR(100) NOT NULL,
    id_utilisateur INT NOT NULL,
    reponse_admin TEXT,
    date_reponse DATE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (id_utilisateur) REFERENCES utilisateur(id) ON DELETE CASCADE,
    INDEX idx_utilisateur (id_utilisateur),
    INDEX idx_statut (statut),
    INDEX idx_priorite (priorite)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Table UTILISATEUR - Amélioration
ALTER TABLE utilisateur ADD COLUMN IF NOT EXISTS statut ENUM('ACTIF', 'INACTIF') DEFAULT 'ACTIF',
ADD COLUMN IF NOT EXISTS telephone VARCHAR(20),
ADD COLUMN IF NOT EXISTS adresse VARCHAR(300),
ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP;

-- Insertion d'un utilisateur admin par défaut (mot de passe: admin123)
INSERT INTO utilisateur (nom, email, mot_de_passe, role) VALUES
('Administrateur', 'admin@event.com', '240be518fabd2724ddb6f04eeb1da5967448d7e831c08c8fa822809f74c720a9', 'ADMIN'),
('User Test', 'user@event.com', '04f8996da763b7a969b1028ee3007569eaf3a635486ddab211d512c85b9df8fb', 'USER');

-- Données de test pour événements
INSERT INTO evenement (titre, description, date_debut, date_fin, lieu, capacite_max, organisateur_id) VALUES
('Conférence Tech 2026', 'Grande conférence sur les nouvelles technologies', '2026-03-15', '2026-03-17', 'Centre de Congrès Tunis', 500, 2),
('Festival Musical', 'Festival de musique internationale', '2026-04-20', '2026-04-22', 'Stade Olympique', 10000, 2),
('Atelier Formation Java', 'Formation intensive JavaFX', '2026-05-10', '2026-05-12', 'Salle Formation IT', 30, 2);

-- Données de test pour réclamations
INSERT INTO reclamation (objet, description, date_creation, statut, priorite, type, id_utilisateur) VALUES
('Problème technique événement', 'Le système de réservation ne fonctionne pas correctement', '2026-02-10', 'EN_ATTENTE', 'HAUTE', 'Technique', 2),
('Demande de remboursement', 'Événement annulé, demande de remboursement', '2026-02-12', 'EN_COURS', 'MOYENNE', 'Financier', 2),
('Question sur capacité', 'Possibilité d\'augmenter la capacité de l\'événement', '2026-02-14', 'RESOLU', 'BASSE', 'Information', 2);
