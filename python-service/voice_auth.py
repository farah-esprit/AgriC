#!/usr/bin/env python3
"""
voice_auth.py
Système d'authentification biométrique par reconnaissance vocale
Utilise librosa pour extraire les MFCC et SQLite pour stocker les empreintes
"""

import sys
import os
import json
import argparse
import warnings
import sqlite3
import numpy as np
from pathlib import Path

warnings.filterwarnings('ignore')

# ══════════════════════════════════════════════════════════════════════════
# VÉRIFICATION DES DÉPENDANCES
# ══════════════════════════════════════════════════════════════════════════
LIBS_OK = True
MISSING_LIB = ""

try:
    import librosa
except ImportError:
    LIBS_OK = False
    MISSING_LIB = "librosa"

try:
    import sounddevice as sd
except ImportError:
    if MISSING_LIB == "":
        LIBS_OK = False
        MISSING_LIB = "sounddevice"

try:
    import soundfile as sf
except ImportError:
    if MISSING_LIB == "":
        LIBS_OK = False
        MISSING_LIB = "soundfile"

# ══════════════════════════════════════════════════════════════════════════
# CONFIGURATION
# ══════════════════════════════════════════════════════════════════════════
SAMPLE_RATE = 16000
DURATION = 3  # secondes
N_MFCC = 13
DB_PATH = Path(__file__).parent / "voice_prints.db"

# ══════════════════════════════════════════════════════════════════════════
# BASE DE DONNÉES
# ══════════════════════════════════════════════════════════════════════════
def init_database():
    """Initialise la base de données SQLite"""
    conn = sqlite3.connect(DB_PATH)
    cursor = conn.cursor()
    cursor.execute('''
        CREATE TABLE IF NOT EXISTS voice_prints (
            username TEXT PRIMARY KEY,
            embedding TEXT NOT NULL,
            created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
        )
    ''')
    conn.commit()
    conn.close()

def save_embedding(username, embedding):
    """Sauvegarde une empreinte vocale"""
    init_database()
    conn = sqlite3.connect(DB_PATH)
    cursor = conn.cursor()
    embedding_json = json.dumps(embedding.tolist())
    cursor.execute('''
        INSERT OR REPLACE INTO voice_prints (username, embedding)
        VALUES (?, ?)
    ''', (username, embedding_json))
    conn.commit()
    conn.close()

def load_embedding(username):
    """Charge une empreinte vocale"""
    init_database()
    conn = sqlite3.connect(DB_PATH)
    cursor = conn.cursor()
    cursor.execute('SELECT embedding FROM voice_prints WHERE username = ?', (username,))
    result = cursor.fetchone()
    conn.close()
    if result:
        return np.array(json.loads(result[0]))
    return None

def list_users():
    """Liste tous les utilisateurs enregistrés"""
    init_database()
    conn = sqlite3.connect(DB_PATH)
    cursor = conn.cursor()
    cursor.execute('SELECT username FROM voice_prints')
    users = [row[0] for row in cursor.fetchall()]
    conn.close()
    return users

def delete_user(username):
    """Supprime un utilisateur"""
    init_database()
    conn = sqlite3.connect(DB_PATH)
    cursor = conn.cursor()
    cursor.execute('DELETE FROM voice_prints WHERE username = ?', (username,))
    conn.commit()
    rows = cursor.rowcount
    conn.close()
    return rows > 0

# ══════════════════════════════════════════════════════════════════════════
# EXTRACTION DES CARACTÉRISTIQUES (MFCC)
# ══════════════════════════════════════════════════════════════════════════
def extract_features(audio_path):
    """Extrait les MFCC d'un fichier audio"""
    try:
        y, sr = librosa.load(audio_path, sr=SAMPLE_RATE)
        mfcc = librosa.feature.mfcc(y=y, sr=sr, n_mfcc=N_MFCC)
        mfcc_mean = np.mean(mfcc, axis=1)
        return mfcc_mean
    except Exception as e:
        print(f"Erreur extraction features: {e}", file=sys.stderr)
        return None

def record_audio(duration=DURATION):
    """Enregistre l'audio depuis le microphone"""
    try:
        print(f"🎤 Enregistrement de {duration} secondes...", file=sys.stderr)
        audio = sd.rec(int(duration * SAMPLE_RATE),
                       samplerate=SAMPLE_RATE,
                       channels=1,
                       dtype='float32')
        sd.wait()
        temp_path = Path(__file__).parent / f"temp_{os.getpid()}.wav"
        sf.write(temp_path, audio, SAMPLE_RATE)
        return str(temp_path)
    except Exception as e:
        print(f"Erreur enregistrement: {e}", file=sys.stderr)
        return None

# ══════════════════════════════════════════════════════════════════════════
# COMPARAISON (Similarité Cosinus)
# ══════════════════════════════════════════════════════════════════════════
def cosine_similarity(a, b):
    """Calcule la similarité cosinus entre deux vecteurs"""
    return np.dot(a, b) / (np.linalg.norm(a) * np.linalg.norm(b))

# ══════════════════════════════════════════════════════════════════════════
# COMMANDES
# ══════════════════════════════════════════════════════════════════════════
def cmd_enroll(args):
    """Enregistre une empreinte vocale"""
    username = args.username

    if args.file:
        audio_path = args.file
    else:
        n_samples = int(args.samples) if args.samples else 3
        embeddings = []
        for i in range(n_samples):
            print(f"Échantillon {i+1}/{n_samples}...", file=sys.stderr)
            audio_path = record_audio(DURATION)
            if not audio_path:
                return {"status": "error", "message": "Erreur enregistrement"}
            features = extract_features(audio_path)
            if features is None:
                return {"status": "error", "message": "Erreur extraction features"}
            embeddings.append(features)
            if os.path.exists(audio_path):
                os.remove(audio_path)
        embedding = np.mean(embeddings, axis=0)
        audio_path = None

    if audio_path:
        embedding = extract_features(audio_path)
        if embedding is None:
            return {"status": "error", "message": "Erreur extraction features"}

    save_embedding(username, embedding)

    return {
        "status": "success",
        "message": f"Empreinte vocale enregistrée pour {username}",
        "username": username
    }

def cmd_verify(args):
    """Vérifie l'identité vocale"""
    username = args.username
    threshold = float(args.threshold) if args.threshold else 0.75

    stored_embedding = load_embedding(username)
    if stored_embedding is None:
        return {"status": "error", "message": f"Aucune empreinte pour {username}", "authenticated": False}

    audio_path = args.file if args.file else record_audio(DURATION)
    temp = not args.file
    if not audio_path:
        return {"status": "error", "message": "Erreur enregistrement"}

    current_embedding = extract_features(audio_path)
    if temp and os.path.exists(audio_path):
        os.remove(audio_path)
    if current_embedding is None:
        return {"status": "error", "message": "Erreur extraction features"}

    similarity = float(cosine_similarity(stored_embedding, current_embedding))
    authenticated = bool(similarity >= threshold)
    confidence = "haute" if similarity >= 0.9 else "moyenne" if similarity >= 0.75 else "faible"

    return {
        "status": "success",
        "authenticated": authenticated,
        "similarity": similarity,
        "threshold": float(threshold),
        "username": username,
        "confidence": confidence,
        "message": "Authentifié" if authenticated else "Non authentifié"
    }

def cmd_identify(args):
    """Identifie l'utilisateur par sa voix"""
    audio_path = args.file if args.file else record_audio(DURATION)
    temp = not args.file
    if not audio_path:
        return {"status": "error", "message": "Erreur enregistrement"}

    current_embedding = extract_features(audio_path)
    if temp and os.path.exists(audio_path):
        os.remove(audio_path)
    if current_embedding is None:
        return {"status": "error", "message": "Erreur extraction features"}

    users = list_users()
    if not users:
        return {"status": "error", "message": "Aucun utilisateur enregistré"}

    best_match, best_similarity = None, 0.0
    for user in users:
        stored_embedding = load_embedding(user)
        if stored_embedding is not None:
            similarity = float(cosine_similarity(stored_embedding, current_embedding))
            if similarity > best_similarity:
                best_similarity = similarity
                best_match = user

    threshold = 0.75
    authenticated = bool(best_similarity >= threshold)
    return {
        "status": "success",
        "authenticated": authenticated,
        "similarity": best_similarity,
        "threshold": threshold,
        "username": best_match if authenticated else "",
        "message": f"Identifié: {best_match}" if authenticated else "Non identifié"
    }

def cmd_list(args):
    """Liste les utilisateurs"""
    users = list_users()
    return {"status": "success", "users": users, "count": len(users)}

def cmd_delete(args):
    """Supprime un utilisateur"""
    username = args.username
    success = delete_user(username)
    return {
        "status": "success" if success else "error",
        "message": f"Utilisateur {username} supprimé" if success else f"Utilisateur {username} introuvable",
        "username": username
    }

def cmd_test(args):
    """Test du système"""
    if not LIBS_OK:
        return {"status": "error", "message": f"Bibliothèque manquante: {MISSING_LIB}", "missing_library": MISSING_LIB}
    return {
        "status": "success",
        "message": "Système opérationnel",
        "sample_rate": SAMPLE_RATE,
        "duration": DURATION,
        "n_mfcc": N_MFCC,
        "database": str(DB_PATH)
    }

# ══════════════════════════════════════════════════════════════════════════
# CONVERTISSEUR JSON POUR TYPES NUMPY
# ══════════════════════════════════════════════════════════════════════════
def convert_numpy(obj):
    if isinstance(obj, (np.bool_, np.bool)):
        return bool(obj)
    if isinstance(obj, (np.float_, np.float32, np.float64)):
        return float(obj)
    if isinstance(obj, (np.int_, np.int32, np.int64)):
        return int(obj)
    return obj

# ══════════════════════════════════════════════════════════════════════════
# MAIN
# ══════════════════════════════════════════════════════════════════════════
def main():
    parser = argparse.ArgumentParser(description='Voice Authentication System')
    parser.add_argument('command', choices=['enroll', 'verify', 'identify', 'list', 'delete', 'test'])
    parser.add_argument('--username', help='Username')
    parser.add_argument('--file', help='Audio file path')
    parser.add_argument('--samples', help='Number of samples for enrollment')
    parser.add_argument('--threshold', help='Similarity threshold')
    parser.add_argument('--json', action='store_true', help='Output as JSON')
    args = parser.parse_args()

    if not LIBS_OK:
        result = {"status": "error", "message": f"Bibliothèque Python manquante: {MISSING_LIB}", "missing_library": MISSING_LIB}
    else:
        commands = {
            'enroll': cmd_enroll,
            'verify': cmd_verify,
            'identify': cmd_identify,
            'list': cmd_list,
            'delete': cmd_delete,
            'test': cmd_test
        }
        result = commands[args.command](args)

    if args.json:
        print(f"JSON_RESULT:{json.dumps(result, default=convert_numpy)}")
    else:
        print(json.dumps(result, indent=2))

if __name__ == '__main__':
    main()