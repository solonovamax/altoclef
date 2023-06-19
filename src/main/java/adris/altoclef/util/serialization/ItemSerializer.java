package adris.altoclef.util.serialization;

import adris.altoclef.util.helpers.ItemHelper;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import net.minecraft.item.Item;

import java.io.IOException;

public class ItemSerializer extends StdSerializer<Item> {
    public ItemSerializer() {
        this(null);
    }

    public ItemSerializer(Class<Item> vc) {
        super(vc);
    }

    @Override
    public void serialize(Item item, JsonGenerator gen, SerializerProvider provider) throws IOException {
        String key = ItemHelper.trimItemName(item.getTranslationKey());
        gen.writeString(key);
    }
}
