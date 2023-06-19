package adris.altoclef.util.serialization;

import adris.altoclef.Debug;
import adris.altoclef.util.helpers.ItemHelper;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;

import java.io.IOException;

public class ItemDeserializer extends StdDeserializer<Item> {
    public ItemDeserializer() {
        this(null);
    }

    public ItemDeserializer(Class<Object> vc) {
        super(vc);
    }

    @Override
    public Item deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        Item item = null;
        if (p.getCurrentToken() == JsonToken.VALUE_NUMBER_INT) {
            // Old raw id (ew stinky)
            int rawId = p.getIntValue();
            item = Item.byRawId(rawId);
        } else {
            // Translation key (the proper way)
            String itemKey = p.getText();
            itemKey = ItemHelper.trimItemName(itemKey);
            Identifier identifier = new Identifier(itemKey);
            if (Registries.ITEM.containsId(identifier)) {
                item = Registries.ITEM.get(identifier);
            } else {
                Debug.logWarning("Invalid item name:" + itemKey + " at " + p.getCurrentLocation().toString());
            }
        }

        return item;
    }
}
