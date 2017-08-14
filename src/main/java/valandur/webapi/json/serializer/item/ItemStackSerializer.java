package valandur.webapi.json.serializer.item;

import org.spongepowered.api.item.inventory.ItemStack;
import valandur.webapi.api.json.WebAPIBaseSerializer;

import java.io.IOException;

public class ItemStackSerializer extends WebAPIBaseSerializer<ItemStack> {
    @Override
    public void serialize(ItemStack value) throws IOException {
        writeStartObject();

        writeField("id", value.getItem().getId());
        writeField("name", value.getTranslation().get());
        writeField("quantity", value.getQuantity());

        if (shouldWriteDetails()) {
            writeObjectFieldStart("data");
            writeData(value);
            writeEndObject();
        }

        writeEndObject();
    }
}