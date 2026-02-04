#!/bin/bash
# Script pour retirer les annotations Swagger de façon sûre

FILES=(
    "reservation/src/main/java/com/example/reservation/controller/PropertyController.java"
    "reservation/src/main/java/com/example/reservation/controller/PropertyAccessCodeController.java"
    "reservation/src/main/java/com/example/reservation/dto/PropertyDto.java"
    "reservation/src/main/java/com/example/reservation/dto/PropertyAccessCodeDto.java"
)

for file in "${FILES[@]}"; do
    echo "Nettoyage de $file..."

    # Retirer @Tag
    sed -i '' '/@Tag(/,/)/d' "$file"

    # Retirer @SecurityRequirement
    sed -i '' '/@SecurityRequirement(/,/)/d' "$file"

    # Retirer @Operation
    sed -i '' '/@Operation(/,/)/d' "$file"

    # Retirer @ApiResponses avec son contenu multiligne
    perl -i -0pe 's/@ApiResponses\([^)]*\{[^}]*\}[^)]*\)\n//gs' "$file"

    # Retirer @Parameter simple
    sed -i '' 's/@Parameter([^)]*) //g' "$file"

    # Retirer @Schema simple sur une seule ligne
    sed -i '' '/@Schema([^)]*)/d' "$file"

    echo "✓ $file nettoyé"
done

echo ""
echo "✅ Nettoyage terminé!"
