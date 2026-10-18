#!/usr/bin/env python3
"""K-FIND 통합식품영양성분정보 원본 CSV에서 안주 적재에 쓰는 값만 뽑는다.

실행:
  python3 -I scripts/import/extract-kfind.py fill        # 이름이 같은 음식의 식품코드를 anjus.csv에 채운다
  python3 -I scripts/import/extract-kfind.py candidates  # 식품코드를 못 채운 안주의 후보 목록을 만든다
  python3 -I scripts/import/extract-kfind.py apply       # 후보 목록에서 pick을 표시한 식품코드를 anjus.csv에 채운다
  python3 -I scripts/import/extract-kfind.py extract     # anjus.csv가 가리키는 식품코드의 영양성분을 nutrition.csv로 뽑는다

원본은 공공데이터포털에서 받은 표준데이터 CSV 두 개다. 132MB가 넘어 커밋하지 않고 data/kfind/에 둔다.
  - 전국통합식품영양성분정보(음식)표준데이터: https://www.data.go.kr/data/15100070/standard.do
  - 전국통합식품영양성분정보(가공식품)표준데이터: https://www.data.go.kr/data/15100066/standard.do

외부 패키지 없이 표준 라이브러리만 쓴다.
"""

import csv
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
DATA_DIR = ROOT / "scripts/import/data/kfind"
FOOD_CSV = DATA_DIR / "식품의약품안전처_통합식품영양성분정보(음식).csv"
PROCESSED_CSV = DATA_DIR / "식품의약품안전처_통합식품영양성분정보(가공식품).csv"
CANDIDATES_CSV = DATA_DIR / "candidates.csv"
ANJUS_CSV = ROOT / "src/main/resources/import/curated/anjus.csv"
NUTRITION_CSV = ROOT / "src/main/resources/import/kfind/nutrition.csv"

# 같은 이름의 음식이 여러 출처에 있으면 실제로 분석한 값을 재료량으로 계산한 값보다 먼저 쓴다.
# 프랜차이즈 출처는 지방이 대부분 비어 있어 무게감을 정할 수 없으므로 쓰지 않는다.
ORIGIN_PRIORITY = [
    "가정식(분석 함량)",
    "외식(분석함량)",
    "외식(재료량 기반 산출함량)",
    "산업체급식(재료량 기반 산출 함량)",
    "중고등학교급식(재료량 기반 산출함량)",
    "초등학교급식(재료량 기반 산출 함량)",
]
CANDIDATE_LIMIT = 5
NUTRITION_HEADER = ["kfind_code", "food_name", "sugar_grams", "fat_grams", "raw_materials"]


def read_csv(path):
    # 원본 CSV는 UTF-8 BOM으로 시작한다.
    with open(path, encoding="utf-8-sig", newline="") as file:
        yield from csv.DictReader(file)


def has_nutrients(row):
    return bool(row["당류(g)"].strip()) and bool(row["지방(g)"].strip())


def usable_foods():
    """우선순위가 있는 출처의 음식 중 당류와 지방이 모두 있는 행을 우선순위, 식품코드 순으로 돌려준다."""
    rank = {origin: index for index, origin in enumerate(ORIGIN_PRIORITY)}
    foods = [
        row
        for row in read_csv(FOOD_CSV)
        if row["식품기원명"] in rank and has_nutrients(row)
    ]
    return sorted(foods, key=lambda row: (rank[row["식품기원명"]], row["식품코드"]))


def usable_processed_foods():
    """가공식품은 모든 행에 지방이 있으므로 당류까지 있는 행만 돌려준다."""
    return [row for row in read_csv(PROCESSED_CSV) if has_nutrients(row)]


def normalize(name):
    return "".join(name.split())


def closest_first(name, foods):
    """이름 길이가 안주 이름에 가까운 음식부터 둔다. 같은 길이면 원래 순서(출처 우선순위)를 지킨다."""
    return sorted(foods, key=lambda food: abs(len(normalize(food["식품명"])) - len(name)))


def read_anjus():
    with open(ANJUS_CSV, encoding="utf-8", newline="") as file:
        reader = csv.DictReader(file)
        return reader.fieldnames, list(reader)


def write_anjus(fieldnames, anjus):
    with open(ANJUS_CSV, "w", encoding="utf-8", newline="") as file:
        writer = csv.DictWriter(file, fieldnames=fieldnames, lineterminator="\n")
        writer.writeheader()
        writer.writerows(anjus)


def fill():
    """식품코드가 빈 안주 중 이름이 같은 음식이 있으면 가장 우선순위가 높은 행의 식품코드를 채운다.

    external_id는 사람이 정한 식별 키라 건드리지 않는다. 식품코드를 고쳐도 적재된 안주의 id가 유지되어야 하기 때문이다.
    """
    best_by_name = {}
    for food in usable_foods():
        best_by_name.setdefault(normalize(food["식품명"]), food)

    fieldnames, anjus = read_anjus()
    filled = 0
    for anju in anjus:
        food = best_by_name.get(normalize(anju["name"]))
        if anju["kfind_code"] or food is None:
            continue
        anju["kfind_code"] = food["식품코드"]
        filled += 1

    write_anjus(fieldnames, anjus)
    remaining = sum(1 for anju in anjus if not anju["kfind_code"])
    print(f"✓ 식품코드를 채운 안주: {filled}개, 아직 빈 안주: {remaining}개")


def candidates_for(anju, foods, processed_foods):
    """후보를 다음 순서로 찾는다.

    1. 이름이 서로 포함되는 음식
    2. 주재료와 조리법(이름 끝 두 글자)이 함께 들어간 음식
    3. 이름이나 첫 주재료가 들어간 가공식품 (치즈, 건어물, 견과처럼 제품으로 먹는 안주)
    """
    name = normalize(anju["name"])
    by_name = [
        food
        for food in foods
        if name in normalize(food["식품명"])
        or (len(normalize(food["식품명"])) >= 2 and normalize(food["식품명"]) in name)
    ]
    if by_name:
        return "이름 포함", closest_first(name, by_name)[:CANDIDATE_LIMIT]

    cooking = name[-2:]
    ingredients = [token for token in anju["main_ingredients"].split("|") if token]
    by_ingredient = [
        food
        for food in foods
        if cooking in food["식품명"]
        and any(ingredient[:2] in food["식품명"] for ingredient in ingredients)
    ]
    if by_ingredient:
        return "주재료와 조리법", closest_first(name, by_ingredient)[:CANDIDATE_LIMIT]

    for keyword in [name] + ingredients[:1]:
        by_product = [food for food in processed_foods if keyword in normalize(food["식품명"])]
        if by_product:
            return "가공식품", closest_first(keyword, by_product)[:CANDIDATE_LIMIT]
    return "후보 없음", []


def candidates():
    """식품코드가 빈 안주마다 후보를 최대 5개까지 candidates.csv에 남긴다. 사람이 고른 코드를 kfind_code에 적는다."""
    foods = usable_foods()
    processed_foods = usable_processed_foods()
    _, anjus = read_anjus()
    rows = []
    for anju in anjus:
        if anju["kfind_code"]:
            continue
        match, found = candidates_for(anju, foods, processed_foods)
        base = {"pick": "", "anju": anju["name"], "source": anju["source"], "match": match}
        if not found:
            rows.append({**base, "rank": "", "kfind_code": "", "food_name": "", "origin": "",
                         "sugar_grams": "", "fat_grams": "", "unit": ""})
        for rank, food in enumerate(found, start=1):
            rows.append({**base, "rank": rank, "kfind_code": food["식품코드"], "food_name": food["식품명"],
                         "origin": food["식품기원명"], "sugar_grams": food["당류(g)"],
                         "fat_grams": food["지방(g)"], "unit": food["영양성분함량기준량"]})

    fieldnames = ["pick", "anju", "source", "match", "rank", "kfind_code", "food_name", "origin",
                  "sugar_grams", "fat_grams", "unit"]
    # 엑셀에서 바로 열리도록 BOM을 붙인다.
    with open(CANDIDATES_CSV, "w", encoding="utf-8-sig", newline="") as file:
        writer = csv.DictWriter(file, fieldnames=fieldnames, lineterminator="\n")
        writer.writeheader()
        writer.writerows(rows)

    anju_count = len({row["anju"] for row in rows})
    no_candidate = sum(1 for row in rows if row["match"] == "후보 없음")
    print(f"✓ 후보 목록: {CANDIDATES_CSV.relative_to(ROOT)} (안주 {anju_count}개, 후보 없음 {no_candidate}개)")


def extract():
    """anjus.csv가 가리키는 식품코드의 100g당 당류와 지방을 nutrition.csv로 뽑는다.

    100ml 기준 행도 100g과 같다고 본다. 구간 경계가 2~25g 단위라 밀도 차이로 등급이 거의 바뀌지 않기 때문이다.
    가공식품의 원재료와 알레르기는 HACCP 포장지표기정보(공공데이터포털 15033307)로 대조한 뒤 채우므로 지금은 비워 둔다.
    """
    _, anjus = read_anjus()
    codes = {anju["kfind_code"] for anju in anjus if anju["kfind_code"]}
    found = {}
    for path in (FOOD_CSV, PROCESSED_CSV):
        for row in read_csv(path):
            if row["식품코드"] in codes:
                found[row["식품코드"]] = row

    NUTRITION_CSV.parent.mkdir(parents=True, exist_ok=True)
    with open(NUTRITION_CSV, "w", encoding="utf-8", newline="") as file:
        writer = csv.writer(file, lineterminator="\n")
        writer.writerow(NUTRITION_HEADER)
        for code in sorted(found):
            row = found[code]
            writer.writerow([code, row["식품명"].strip(), row["당류(g)"].strip(), row["지방(g)"].strip(), ""])

    missing = sorted(codes - found.keys())
    print(f"✓ 영양성분을 뽑은 식품코드: {len(found)}개 → {NUTRITION_CSV.relative_to(ROOT)}")
    if missing:
        print(f"✗ 원본에 없는 식품코드: {missing}", file=sys.stderr)
        sys.exit(1)


def apply():
    """candidates.csv에서 pick 칸에 아무 값이나 적은 행의 식품코드를 그 안주의 kfind_code로 채운다. external_id는 건드리지 않는다.

    한 안주에 pick을 두 개 이상 적으면 어느 쪽인지 알 수 없어 아무것도 채우지 않고 실패한다.
    pick이 없는 안주는 비워 두며, 적재에서 MISSING_KFIND_CODE로 제외된다.
    """
    picks = {}
    duplicated = set()
    for row in read_csv(CANDIDATES_CSV):
        if not row["pick"].strip() or not row["kfind_code"]:
            continue
        if row["anju"] in picks:
            duplicated.add(row["anju"])
        picks[row["anju"]] = row["kfind_code"]
    if duplicated:
        print(f"✗ pick을 두 개 이상 표시한 안주: {sorted(duplicated)}", file=sys.stderr)
        sys.exit(1)

    fieldnames, anjus = read_anjus()
    applied = 0
    for anju in anjus:
        code = picks.get(anju["name"])
        if anju["kfind_code"] or code is None:
            continue
        anju["kfind_code"] = code
        applied += 1

    write_anjus(fieldnames, anjus)
    remaining = sum(1 for anju in anjus if not anju["kfind_code"])
    print(f"✓ pick으로 채운 안주: {applied}개, 아직 빈 안주: {remaining}개")


COMMANDS = {"fill": fill, "candidates": candidates, "apply": apply, "extract": extract}

if __name__ == "__main__":
    if len(sys.argv) != 2 or sys.argv[1] not in COMMANDS:
        print(f"사용법: {sys.argv[0]} {{{'|'.join(COMMANDS)}}}", file=sys.stderr)
        sys.exit(2)
    for path in (FOOD_CSV, PROCESSED_CSV):
        if not path.exists():
            print(f"✗ 원본 CSV가 없습니다: {path.relative_to(ROOT)}", file=sys.stderr)
            sys.exit(1)
    COMMANDS[sys.argv[1]]()
