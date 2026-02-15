package madoku.craft.compat.mixin.tools;

import com.google.gson.JsonObject;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(targets = "madoku.craft.tools.system.CustomToolsConfig")
public class CustomToolsConfigCompatMixin {
	private static final String[] ARMOR_PIECES = {"helmet", "chestplate", "leggings", "boots"};
	private static final String[] ARMOR_MATERIALS = {"leather", "copper", "iron", "golden", "diamond", "netherite"};
	private static final int[] DURABILITY = {192, 256, 384, 512, 768, 1024};
	private static final double[] ARMOR = {0.5d, 1.0d, 1.5d, 2.0d, 2.5d, 3.0d};
	private static final double[] TOUGHNESS = {2.5d, 5.0d, 7.5d, 10.0d, 12.5d, 15.0d};

	@Inject(method = "update", at = @At("HEAD"), remap = false)
	private void madokuCompat$ensureArmorValuesAtLoad(JsonObject root, CallbackInfoReturnable<Boolean> cir) {
		if (root == null) {
			return;
		}

		JsonObject overrides = getOrCreateObject(root, "overrides");
		for (String piece : ARMOR_PIECES) {
			JsonObject pieceRoot = getOrCreateObject(overrides, piece);
			ensureArmorPieceValues(pieceRoot, piece);
		}
	}

	@Inject(method = "buildArmorPieceDefaults", at = @At("RETURN"), cancellable = true, remap = false)
	private static void madokuCompat$ensureArmorDefaults(String suffix, CallbackInfoReturnable<JsonObject> cir) {
		if (!isArmorPieceSuffix(suffix)) {
			return;
		}

		JsonObject category = cir.getReturnValue();
		ensureArmorPieceValues(category, suffix);
		cir.setReturnValue(category);
	}

	private static JsonObject getOrCreateObject(JsonObject parent, String key) {
		if (parent.has(key) && parent.get(key).isJsonObject()) {
			return parent.getAsJsonObject(key);
		}
		JsonObject created = new JsonObject();
		parent.add(key, created);
		return created;
	}

	private static boolean isArmorPieceSuffix(String suffix) {
		for (String piece : ARMOR_PIECES) {
			if (piece.equals(suffix)) {
				return true;
			}
		}
		return false;
	}

	private static void ensureArmorPieceValues(JsonObject category, String piece) {
		for (int i = 0; i < ARMOR_MATERIALS.length; i++) {
			String id = "minecraft:" + ARMOR_MATERIALS[i] + "_" + piece;
			JsonObject entry = getOrCreateObject(category, id);
			ensureNumber(entry, "durability", DURABILITY[i]);
			ensureNumber(entry, "armor", ARMOR[i]);
			ensureNumber(entry, "armorToughness", TOUGHNESS[i]);
		}
	}

	private static void ensureNumber(JsonObject parent, String key, Number value) {
		if (parent.has(key) && parent.get(key).isJsonPrimitive()
				&& parent.getAsJsonPrimitive(key).isNumber()) {
			return;
		}
		parent.addProperty(key, value);
	}
}
