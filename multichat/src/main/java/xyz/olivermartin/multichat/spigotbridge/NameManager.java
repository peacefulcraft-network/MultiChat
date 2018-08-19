package xyz.olivermartin.multichat.spigotbridge;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.spongepowered.api.event.Listener;

/**
 * Player Name Manager
 * <p>Manages players names, uuids and nicknames</p>
 * 
 * @author Oliver Martin (Revilo410)
 *
 */
public class NameManager {

	private static NameManager instance;

	static {
		instance = new NameManager();
	}

	public static NameManager getInstance() {
		return instance;
	}

	// END OF STATIC DEFINITIONS

	private Map<UUID,String> mapUUIDNick;
	private Map<UUID,String> mapUUIDName;
	private Map<String,UUID> mapNickUUID;
	private Map<String,UUID> mapNameUUID;

	private Map<String,String> mapNickFormatted;
	private Map<String,String> mapNameFormatted;

	private List<UUID> online;

	private NameManager() {

		setDefaultData();

		online = new ArrayList<UUID>();

	}

	private void setDefaultData() {

		mapUUIDNick = new HashMap<UUID,String>();
		mapUUIDName = new HashMap<UUID,String>();
		mapNickUUID = new HashMap<String,UUID>();
		mapNameUUID = new HashMap<String,UUID>();

		mapNickFormatted = new HashMap<String,String>();
		mapNameFormatted = new HashMap<String,String>();

	}

	/**
	 * Returns the FORMATTED NICKNAME of a player if they have one set, otherwise returns their username
	 * 
	 * @param uuid The Unique ID of the player to lookup
	 * @return The NICKNAME of the player if it is set, otherwise their username
	 */
	public String getCurrentName(UUID uuid) {

		synchronized (mapUUIDNick) {
			if (mapUUIDNick.containsKey(uuid)) {
				return mapNickFormatted.get(mapUUIDNick.get(uuid));
			} 
		}

		synchronized (mapUUIDName) {
			if (mapUUIDName.containsKey(uuid)) {
				return mapNameFormatted.get(mapUUIDName.get(uuid));
			}
		}

		return "";
	}

	/**
	 * Returns the username of a player
	 * 
	 * @param uuid The Unique ID of the player to lookup
	 * @return The username of the player
	 */
	public String getName(UUID uuid) {

		synchronized (mapUUIDName) {
			if (mapUUIDName.containsKey(uuid)) {
				return mapNameFormatted.get(mapUUIDName.get(uuid));
			}
		}

		return "";

	}

	/**
	 * Gets the UUID of a player from their UNFORMATTED nickname
	 * THIS MEANS THE NICKNAME PROVIDED MUST BE IN LOWERCASE WITH ALL FORMATTING CODES REMOVED
	 * 
	 * @param nickname The UNFORMATTED nickname of the player
	 * @return An optional which may contain their UUID if the nickname was found in the system
	 */
	private Optional<UUID> getUUIDFromUnformattedNickname(String nickname) {

		nickname = nickname.toLowerCase();

		synchronized (mapNickUUID) {
			if (mapNickUUID.containsKey(nickname)) {
				return Optional.of(mapNickUUID.get(nickname));
			}
		}

		return Optional.empty();

	}

	/**
	 * Returns a player's UUID given their username
	 * 
	 * @param username The player's username
	 * @return An optional value which may contain their UUID if the username was found
	 */
	public Optional<UUID> getUUIDFromName(String username) {

		username = username.toLowerCase();

		synchronized (mapNameUUID) {
			if (mapNameUUID.containsKey(username)) {
				return Optional.of(mapNameUUID.get(username));
			}
		}

		return Optional.empty();

	}

	/**
	 * Gets a player's UUID from their nickname
	 * 
	 * @param nickname The player's nickname (which may contain formatting codes etc.)
	 * @return An optional value which may contain their UUID if the nickname was found
	 */
	public Optional<UUID> getUUIDFromNickname(String nickname) {

		nickname = nickname.toLowerCase();
		nickname = ChatColor.stripColor(nickname);

		return getUUIDFromUnformattedNickname(nickname);

	}

	/**
	 * Gets a player's username from their nickname
	 * 
	 * @param nickname The player's nickname (which may contain formatting codes etc.)
	 * @return An optional value which may contain their username if the nickname was found
	 */
	public Optional<String> getNameFromNickname(String nickname) {

		Optional<UUID> oUUID = getUUIDFromNickname(nickname);

		if (!oUUID.isPresent()) {
			return Optional.empty();
		}

		UUID uuid = oUUID.get();

		return Optional.of(getName(uuid));

	}

	/**
	 * Gets a player's nickname from their username
	 * 
	 * @param nickname The player's username
	 * @return An optional value which may contain their nickname, or their username if a nickname was not set, as long as their username can be found
	 */
	public Optional<String> getCurrentNameFromName(String username) {

		username = username.toLowerCase();

		Optional<UUID> oUUID = getUUIDFromName(username);

		if (!oUUID.isPresent()) {
			return Optional.empty();
		}

		UUID uuid = oUUID.get();

		return Optional.of(getCurrentName(uuid));

	}

	/**
	 * Register a player as online
	 * <p>Also performs any setup needed to equip nicknames etc.</p>
	 * @param player
	 */
	public void registerPlayer(Player player) {

		UUID uuid = player.getUniqueId();
		String username = player.getName();
		String oldUsername;

		synchronized (mapUUIDName) {

			if (mapUUIDName.containsKey(uuid)) {

				oldUsername = mapUUIDName.get(uuid);

				if (!oldUsername.equalsIgnoreCase(username)) {

					synchronized (mapNameUUID) {

						mapUUIDName.remove(uuid);
						mapUUIDName.put(uuid, username.toLowerCase());
						mapNameUUID.remove(oldUsername);
						mapNameUUID.put(username.toLowerCase(), uuid);

					}

				}

				mapNameFormatted.remove(oldUsername);
				mapNameFormatted.put(username.toLowerCase(), username);

			} else {

				synchronized (mapNameUUID) {

					mapUUIDName.put(uuid, username.toLowerCase());
					mapNameUUID.put(username.toLowerCase(), uuid);
					mapNameFormatted.put(username.toLowerCase(), username);

				}

			}

		}

		online.add(uuid);
		System.out.println("[MultiChat] [SPIGOT] [+] " + username + " has joined this server.");

	}

	/**
	 * Register a player as offline
	 * @param player
	 */
	public void unregisterPlayer(Player player) {

		online.remove(player.getUniqueId());
		System.out.println("[MultiChat] [SPIGOT] [-] " + player.getName() + " has left this server.");

	}

	/**
	 * Set the nickname of a player
	 * @param uuid
	 * @param nickname
	 */
	public void setNickname(UUID uuid, String nickname) {

		if (!mapUUIDName.containsKey(uuid)) {
			return;
		}

		String unformattedNickname = ChatColor.stripColor(nickname.toLowerCase());

		synchronized (mapNickUUID) {

			// Check for duplicates
			if (mapNickUUID.containsKey(unformattedNickname)) {
				if (mapNickUUID.get(unformattedNickname) != uuid) {
					return;
				}
			}

			mapUUIDNick.put(uuid, unformattedNickname);
			mapNickUUID.put(unformattedNickname, uuid);
			mapNickFormatted.put(unformattedNickname, nickname);

		}

	}

	/**
	 * @param username
	 * @return If this player has logged into the server before
	 */
	public boolean existsPlayer(String username) {
		return mapNameUUID.containsKey(username.toLowerCase());
	}

	/**
	 * @param nickname
	 * @return If this nickname is currently in use
	 */
	public boolean existsNickname(String nickname) {
		return mapNickUUID.containsKey(ChatColor.stripColor(nickname.toLowerCase()));
	}

	/**
	 * @param uuid
	 * @return If this player is currently online on the server
	 */
	public boolean isOnline(UUID uuid) {
		return online.contains(uuid);
	}

	/**
	 * Removes the nickname for a specified player
	 * @param uuid
	 */
	public void removeNickname(UUID uuid) {

		synchronized (mapUUIDNick) {

			if (!mapUUIDNick.containsKey(uuid)) {
				return;
			}

			String nickname = mapUUIDNick.get(uuid);

			mapUUIDNick.remove(uuid);
			mapNickUUID.remove(nickname);
			mapNickFormatted.remove(nickname);

		}

	}

	/**
	 * Save all persistent nickname data to file
	 * 
	 * @param saveFile The file output stream to use
	 */
	public void saveNicknameData(FileOutputStream saveFile) {

		try {

			ObjectOutputStream out = new ObjectOutputStream(saveFile);

			out.writeObject(mapUUIDNick);
			out.writeObject(mapUUIDName);
			out.writeObject(mapNickUUID);
			out.writeObject(mapNameUUID);
			out.writeObject(mapNickFormatted);
			out.writeObject(mapNameFormatted);

			out.close();
			System.out.println("[MultiChatBridge] The nicknames file was successfully saved!");

		} catch (IOException e) {

			System.out.println("[MultiChatBridge] An error has occured writing the nicknames file!");
			e.printStackTrace();

		}

	}

	//	/**
	//	 * Save all persistent nickname data to file
	//	 * 
	//	 * @param configLoader Configuration file loader to use
	//	 */
	//	@SuppressWarnings("serial")
	//	public void SPONGE_saveNicknameData(ConfigurationLoader<CommentedConfigurationNode> configLoader) {
	//
	//		ConfigurationNode rootNode;
	//
	//		rootNode = configLoader.createEmptyNode();
	//
	//		try {
	//
	//			rootNode.getNode("nickname_uuidnick").setValue(new TypeToken<Map<UUID,String>>() {}, mapUUIDNick);
	//			rootNode.getNode("nickname_uuidname").setValue(new TypeToken<Map<UUID,String>>() {}, mapUUIDName);
	//			rootNode.getNode("nickname_nickuuid").setValue(new TypeToken<Map<String,UUID>>() {}, mapNickUUID);
	//			rootNode.getNode("nickname_nameuuid").setValue(new TypeToken<Map<String,UUID>>() {}, mapNameUUID);
	//			rootNode.getNode("nickname_nickformatted").setValue(new TypeToken<Map<String,String>>() {}, mapNickFormatted);
	//			rootNode.getNode("nickname_nameformatted").setValue(new TypeToken<Map<String,String>>() {}, mapNameFormatted);
	//
	//			try {
	//
	//				configLoader.save(rootNode);
	//
	//			} catch (IOException e) {
	//				e.printStackTrace();
	//			}
	//
	//		} catch (ObjectMappingException e) {
	//
	//			e.printStackTrace();
	//			System.err.println("[MultiChatSponge] ERROR: Could not write nicknames :(");
	//
	//		}
	//
	//	}

	/**
	 * Load (or attempt to load) nickname data saved to file
	 * 
	 * @param saveFile The file input stream to use
	 */
	@SuppressWarnings({ "unchecked" })
	public void loadNicknameData(FileInputStream saveFile) {

		try {

			ObjectInputStream in = new ObjectInputStream(saveFile);

			mapUUIDNick = (Map<UUID, String>) in.readObject();
			mapUUIDName = (Map<UUID, String>) in.readObject();
			mapNickUUID = (Map<String, UUID>) in.readObject();
			mapNameUUID = (Map<String, UUID>) in.readObject();
			mapNickFormatted = (Map<String, String>) in.readObject();
			mapNameFormatted = (Map<String, String>) in.readObject();

			in.close();

			System.out.println("[MultiChatBridge] The nicknames file was successfully loaded!");

		} catch (IOException|ClassNotFoundException e) {

			System.out.println("[MultiChatBridge] An error has occured reading the nicknames file!");
			e.printStackTrace();

		}

	}

	//	/**
	//	 * Load (or attempt to load) nickname data saved to file
	//	 * 
	//	 * @param configLoader The configuration loader to use
	//	 */
	//	@SuppressWarnings("serial")
	//	public void SPONGE_loadNicknameData(ConfigurationLoader<CommentedConfigurationNode> configLoader) {
	//
	//		ConfigurationNode rootNode;
	//
	//		try {
	//
	//			rootNode = configLoader.load();
	//
	//			try {
	//
	//				mapUUIDNick = (Map<UUID, String>) rootNode.getNode("nickname_uuidnick").getValue(new TypeToken<Map<UUID,String>>() { /* EMPTY */ });
	//				mapUUIDName = (Map<UUID, String>) rootNode.getNode("nickname_uuidname").getValue(new TypeToken<Map<UUID,String>>() { /* EMPTY */ });
	//				mapNickUUID = (Map<String, UUID>) rootNode.getNode("nickname_nickuuid").getValue(new TypeToken<Map<String, UUID>>() { /* EMPTY */ });
	//				mapNameUUID = (Map<String, UUID>) rootNode.getNode("nickname_nameuuid").getValue(new TypeToken<Map<String, UUID>>() { /* EMPTY */ });
	//				mapNickFormatted = (Map<String, String>) rootNode.getNode("nickname_nickformatted").getValue(new TypeToken<Map<String, String>>() { /* EMPTY */ });
	//				mapNameFormatted = (Map<String, String>) rootNode.getNode("nickname_nameformatted").getValue(new TypeToken<Map<String, String>>() { /* EMPTY */ });
	//
	//				if (mapUUIDNick == null) mapUUIDNick = new HashMap<UUID,String>();
	//				if (mapUUIDName == null) mapUUIDName = new HashMap<UUID,String>();
	//				if (mapNickUUID == null) mapNickUUID = new HashMap<String,UUID>();
	//				if (mapNameUUID == null) mapNameUUID = new HashMap<String,UUID>();
	//				if (mapNickFormatted == null) mapNickFormatted = new HashMap<String,String>();
	//				if (mapNameFormatted == null) mapNameFormatted = new HashMap<String,String>();
	//
	//
	//				System.out.println("[MultiChatSponge] Nickname data loaded");
	//
	//			} catch (ClassCastException e) {
	//
	//				setDefaultData();
	//
	//			} catch (ObjectMappingException e) {
	//
	//				setDefaultData();
	//
	//			}
	//
	//			try {
	//
	//				configLoader.save(rootNode);
	//
	//			} catch (IOException e) {
	//
	//				e.printStackTrace();
	//
	//			}
	//
	//		} catch (IOException e) {
	//
	//			e.printStackTrace();
	//			setDefaultData();
	//
	//		}
	//
	//	}

	/*
	 * EVENT LISTENERS
	 */

	@Listener
	public void onLogin(PlayerLoginEvent event) {

		registerPlayer(event.getPlayer());

	}

	@Listener
	public void onLogout(PlayerQuitEvent event) {

		unregisterPlayer(event.getPlayer());

	}

}
