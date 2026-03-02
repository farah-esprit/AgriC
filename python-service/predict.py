import argparse
import json
import re
import unicodedata


def normalize(value):
    text = (value or "").strip().lower()
    text = unicodedata.normalize("NFKD", text)
    text = "".join(ch for ch in text if not unicodedata.combining(ch))
    return text


def parse_number(value):
    text = normalize(value).replace(",", ".")
    match = re.search(r"-?\d+(?:\.\d+)?", text)
    if not match:
        return None
    try:
        return float(match.group(0))
    except ValueError:
        return None


def has_any(text, words):
    return any(word in text for word in words)


def infer_disease(etat, symptomes, plant_type, leaf_color, humidity, temperature):
    context = " ".join([etat, symptomes, plant_type, leaf_color])
    candidates = []

    is_tomato = has_any(plant_type, ["tomate", "tomato"])
    humidity_val = parse_number(humidity)
    temp_val = parse_number(temperature)

    score_mildiou = 0
    reasons_mildiou = []
    if has_any(context, ["tache", "brun", "noir", "marron"]):
        score_mildiou += 2
        reasons_mildiou.append("presence de taches brunes/noires")
    if humidity_val is not None and humidity_val >= 75:
        score_mildiou += 2
        reasons_mildiou.append("humidite elevee")
    if temp_val is not None and 14 <= temp_val <= 26:
        score_mildiou += 1
        reasons_mildiou.append("temperature compatible avec mildiou")
    if is_tomato and score_mildiou > 0:
        candidates.append(("Mildiou probable", score_mildiou, reasons_mildiou))

    score_oidium = 0
    reasons_oidium = []
    if has_any(context, ["blanc", "poudre", "duvet"]):
        score_oidium += 3
        reasons_oidium.append("traces blanches/poudreuses")
    if humidity_val is not None and 45 <= humidity_val <= 75:
        score_oidium += 1
        reasons_oidium.append("humidite moderee")
    if temp_val is not None and 18 <= temp_val <= 30:
        score_oidium += 1
        reasons_oidium.append("temperature favorable")
    if score_oidium > 0:
        candidates.append(("Oidium probable", score_oidium, reasons_oidium))

    score_stress = 0
    reasons_stress = []
    if has_any(context, ["fletri", "fane", "sec", "mou"]):
        score_stress += 2
        reasons_stress.append("symptomes de deshydratation")
    if humidity_val is not None and humidity_val < 35:
        score_stress += 2
        reasons_stress.append("humidite faible")
    if temp_val is not None and temp_val > 30:
        score_stress += 1
        reasons_stress.append("temperature elevee")
    if score_stress > 0:
        candidates.append(("Stress hydrique", score_stress, reasons_stress))

    score_carence = 0
    reasons_carence = []
    if has_any(context, ["jaune", "jaun", "chlorose"]):
        score_carence += 2
        reasons_carence.append("jaunissement/chlorose")
    if has_any(context, ["croissance lente", "petite feuille"]):
        score_carence += 1
        reasons_carence.append("croissance faible")
    if score_carence > 0:
        candidates.append(("Carence nutritionnelle probable", score_carence, reasons_carence))

    score_mosaique = 0
    reasons_mosaique = []
    if has_any(context, ["mosaique", "deformation", "frise", "frisure"]):
        score_mosaique += 3
        reasons_mosaique.append("motifs/deformations foliaires")
    if score_mosaique > 0:
        candidates.append(("Virus de la mosaique (suspect)", score_mosaique, reasons_mosaique))

    if not candidates:
        disease = "Aucune maladie clairement identifiee"
        confidence = 35.0
        report = (
            "Analyse preliminaire: les informations fournies ne permettent pas "
            "d'identifier une maladie avec certitude. "
            "Surveiller 48h et ajouter des symptomes plus precis (taches, couleur, evolution)."
        )
        return disease, confidence, report

    disease, score, reasons = sorted(candidates, key=lambda x: x[1], reverse=True)[0]
    confidence = min(95.0, 52.0 + (score * 11.5))
    recommendation = (
        "Recommandation: isoler les plants atteints, supprimer les feuilles fortement touchees, "
        "adapter l'arrosage et confirmer par un expert/agronome."
    )
    report = (
        f"Diagnostic probable: {disease}. "
        f"Indices: {', '.join(dict.fromkeys(reasons))}. "
        f"Humidite={humidity or 'N/A'}%, Temperature={temperature or 'N/A'}C. "
        f"{recommendation}"
    )
    return disease, confidence, report


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--etat", default="")
    parser.add_argument("--symptomes", default="")
    parser.add_argument("--plantType", default="")
    parser.add_argument("--leafColor", default="")
    parser.add_argument("--humidity", default="")
    parser.add_argument("--temperature", default="")
    args = parser.parse_args()

    disease, confidence, report = infer_disease(
        etat=normalize(args.etat),
        symptomes=normalize(args.symptomes),
        plant_type=normalize(args.plantType),
        leaf_color=normalize(args.leafColor),
        humidity=args.humidity,
        temperature=args.temperature,
    )

    print(json.dumps({
        "disease": disease,
        "confidence": round(confidence, 2),
        "rapportIa": report
    }, ensure_ascii=False))


if __name__ == "__main__":
    main()
