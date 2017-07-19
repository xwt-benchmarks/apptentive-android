package com.apptentive.android.sdk.conversation;

import com.apptentive.android.sdk.serialization.SerializableObject;
import com.apptentive.android.sdk.util.StringUtils;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Class which represents all conversation entries stored on the disk
 */
public class ConversationMetadata implements SerializableObject, Iterable<ConversationMetadataItem> {
	private static final byte VERSION = 1;

	private final List<ConversationMetadataItem> items;

	public ConversationMetadata() {
		items = new ArrayList<>();
	}

	//region Serialization

	public ConversationMetadata(DataInput in) throws IOException {
		byte version = in.readByte();
		if (version != VERSION) {
			throw new IOException("Expected version " + VERSION + " but was " + version);
		}

		int count = in.readByte();
		items = new ArrayList<>(count);
		for (int i = 0; i < count; ++i) {
			items.add(new ConversationMetadataItem(in));
		}
	}

	@Override
	public void writeExternal(DataOutput out) throws IOException {
		out.writeByte(VERSION);
		out.write(items.size());
		for (int i = 0; i < items.size(); ++i) {
			items.get(i).writeExternal(out);
		}
	}

	//endregion

	//region Items

	void addItem(ConversationMetadataItem item) {
		items.add(item);
	}

	ConversationMetadataItem findItem(final ConversationState state) {
		return findItem(new Filter() {
			@Override
			public boolean accept(ConversationMetadataItem item) {
				return state.equals(item.state);
			}
		});
	}

	ConversationMetadataItem findItem(final Conversation conversation) {
		return findItem(new Filter() {
			@Override
			public boolean accept(ConversationMetadataItem item) {
				return StringUtils.equal(item.localConversationId, conversation.getLocalIdentifier());
			}
		});
	}

	ConversationMetadataItem findItem(Filter filter) {
		for (ConversationMetadataItem item : items) {
			if (filter.accept(item)) {
				return item;
			}
		}
		return null;
	}

	//endregion

	//region Iterable

	@Override
	public Iterator<ConversationMetadataItem> iterator() {
		return items.iterator();
	}

	//endregion

	//region Getters/Setters

	public boolean hasItems() {
		return items.size() > 0;
	}

	public List<ConversationMetadataItem> getItems() {
		return items;
	}

	//endregion

	//region Filter

	public interface Filter {
		boolean accept(ConversationMetadataItem item);
	}

	//endregion


	@Override
	public String toString() {
		return "ConversationMetadata{" +
			       "items=" + items +
			       '}';
	}
}