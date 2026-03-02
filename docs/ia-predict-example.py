"""
Exemple de script Python pour le diagnostic IA (AgriConnect).
À placer dans votre dossier IA (ex: C:\\Users\\souei\\Downloads\\IA\\IA\\predict.py).
L'application Java appelle ce script avec les arguments ci-dessous et attend
une seule ligne JSON sur stdout.

Usage attendu par Java:
  python predict.py --etat "..." --symptomes "..." --plantType "..." --leafColor "..." --humidity "..." --temperature "..."

Sortie attendue (une ligne JSON):
  {"disease": "Nom maladie", "confidence": 0.85, "rapportIa": "Rapport détaillé..."}
  - disease: nom de la maladie détectée (ou "Aucune" / "Non déterminé")
  - confidence: entre 0 et 1 (ou 0-100 selon votre convention, Java gère les deux)
  - rapportIa: texte libre du rapport
"""

import argparse
import json
import sys

def main():
    p = argparse.ArgumentParser()
    p.add_argument("--etat", default="")
    p.add_argument("--symptomes", default="")
    p.add_argument("--plantType", default="")
    p.add_argument("--leafColor", default="")
    p.add_argument("--humidity", default="")
    p.add_argument("--temperature", default="")
    args = p.parse_args()

    # TODO: ici appeler votre modèle (charger le modèle, prédire à partir des champs)
    # Exemple factice:
    disease = "À confirmer (exemple)"
    confidence = 0.0
    rapport = f"État: {args.etat}. Symptômes: {args.symptomes}. Type: {args.plantType}, Feuilles: {args.leafColor}, H%: {args.humidity}, T°C: {args.temperature}."

    out = {
        "disease": disease,
        "confidence": confidence,
        "rapportIa": rapport
    }
    print(json.dumps(out, ensure_ascii=False))

if __name__ == "__main__":
    main()
