package nl.pim16aap2.bigDoors.GUI;

import com.cryptomorin.xseries.XMaterial;
import com.google.common.collect.Lists;
import nl.pim16aap2.bigDoors.BigDoors;
import nl.pim16aap2.bigDoors.BigDoors.MCVersion;
import nl.pim16aap2.bigDoors.Door;
import nl.pim16aap2.bigDoors.util.DoorAttribute;
import nl.pim16aap2.bigDoors.util.DoorDirection;
import nl.pim16aap2.bigDoors.util.DoorOwner;
import nl.pim16aap2.bigDoors.util.DoorType;
import nl.pim16aap2.bigDoors.util.Messages;
import nl.pim16aap2.bigDoors.util.PageType;
import nl.pim16aap2.bigDoors.util.RotateDirection;
import nl.pim16aap2.bigDoors.util.Util;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.*;
import java.util.regex.Pattern;

@SuppressWarnings("deprecation")
public class GUI
{
    private static final Material PAGESWITCHMAT = Material.ARROW;
    private static final Material CURRDOORMAT = Material.BOOK;
    private static final Material CHANGETIMEMAT = XMaterial.CLOCK.parseMaterial();
    private static final Material NEWDOORMAT = XMaterial.WRITABLE_BOOK.parseMaterial();
    private static final Material LOCKDOORMAT = XMaterial.GREEN_STAINED_GLASS_PANE.parseMaterial();
    private static final Material UNLOCKDOORMAT = XMaterial.RED_STAINED_GLASS_PANE.parseMaterial();
    private static final Material CONFIRMMAT = XMaterial.RED_STAINED_GLASS_PANE.parseMaterial();
    private static final Material NOTCONFIRMMAT = XMaterial.GREEN_STAINED_GLASS_PANE.parseMaterial();
    private static final Material TOGGLEDOORMAT = Material.LEVER;
    private static final Material INFOMAT = Material.BOOKSHELF;
    private static final Material DELDOORMAT = Material.BARRIER;
    private static final Material RELOCATEPBMAT = Material.LEATHER_BOOTS;
    private static final Material SETOPENDIRMAT = Material.COMPASS;
    private static final Material SETBTMOVEMAT = XMaterial.STICKY_PISTON.parseMaterial();
    private static final Material ADDOWNERMAT = XMaterial.PLAYER_HEAD.parseMaterial();
    private static final Material REMOVEOWNERMAT = XMaterial.SKELETON_SKULL.parseMaterial();
    private static final Material NOTIFICATIONSMAT_ON = XMaterial.MUSIC_DISC_STAL.parseMaterial();
    private static final Material NOTIFICATIONSMAT_OFF = XMaterial.MUSIC_DISC_11.parseMaterial();
    private static final Material BYPASS_PROTECTIONS_ON = XMaterial.TNT.parseMaterial();
    private static final Material BYPASS_PROTECTIONS_OFF = XMaterial.DIAMOND.parseMaterial();
    private static final ItemStack SEARCH = XMaterial.COMPASS.parseItem();
    private static final byte LOCKEDDATA = 14;
    private static final byte UNLOCKEDDATA = 5;
    private static final byte CONFIRMDATA = 14;
    private static final byte NOTCONFIRMDATA = 5;
    private static final byte PLAYERHEADDATA = 3;
    private static final byte SKULLDATA = 0;
    private static final int CHESTSIZE = 45;
    private static Material[] DOORTYPES = new Material[4];

    static
    {
        // Ugly hack. I cannot be bothered to fix this properly.
        if (MCVersion.v1_11_R1.equals(BigDoors.getMCVersion()) || MCVersion.v1_12_R1.equals(BigDoors.getMCVersion()))
        {
            DOORTYPES[0] = Material.getMaterial("WOOD_DOOR"); // Door
            DOORTYPES[1] = Material.getMaterial("TRAP_DOOR"); // DrawBridge
            DOORTYPES[2] = Material.getMaterial("IRON_DOOR"); // Portcullis
            DOORTYPES[3] = Material.getMaterial("PISTON_BASE"); // Sliding Door
        }
        else
        {
            DOORTYPES[0] = XMaterial.OAK_DOOR.parseMaterial(); // Door
            DOORTYPES[1] = XMaterial.OAK_TRAPDOOR.parseMaterial(); // DrawBridge
            DOORTYPES[2] = XMaterial.IRON_DOOR.parseMaterial(); // Portcullis
            DOORTYPES[3] = XMaterial.PISTON.parseMaterial(); // Sliding Door
        }
    }

    private static final Pattern newLines = Pattern.compile("\\\n");

    private final BigDoors plugin;
    private final Messages messages;
    private final Player player;

    @SuppressWarnings("unused")
    private int missingHeadTextures;

    private PageType pageType;
    private int page;
    private ArrayList<Door> doors;
    private ArrayList<DoorOwner> owners;
    private int doorOwnerPage = 0;
    private int maxDoorOwnerPageCount = 0;
    private boolean sortAlphabetically = false;
    private Inventory inventory = null;
    private final Map<Integer, GUIItem> items;
    private int maxPageCount;
    private Door door = null;

    public GUI(BigDoors plugin, Player player)
    {
        missingHeadTextures = 0;
        this.plugin = plugin;
        messages = plugin.getMessages();
        this.player = player;

        pageType = PageType.DOORLIST;
        page = 0;
        items = new HashMap<>();

        doors = Lists.newArrayList(plugin.getCommander().getDoors());

        sort();
        update();
    }

    public GUI(BigDoors plugin, Player player, String doorName) {
        missingHeadTextures = 0;
        this.plugin = plugin;
        messages = plugin.getMessages();
        this.player = player;

        pageType = PageType.DOORLIST;
        page = 0;
        items = new HashMap<>();

        long id = plugin.getCommander().getDoorUIDByName(doorName);
        if (id == 0) {
            close();
            Util.messagePlayer(player, "§e§lBigDoors §8| §7Non esiste nessuna porta con questo nome.");
            return;
        }

        doors = Lists.newArrayList(plugin.getCommander().getDoor(player.getUniqueId(), id, true));

        sort();
        update();
    }

    private void addLore(final ArrayList<String> lore, final String toAdd)
    {
        lore.addAll(Arrays.asList(newLines.split(toAdd)));
    }

    private void update()
    {
        if (!(pageType == PageType.DOORLIST || pageType == PageType.DOORCREATION))
            isStillOwner();

        items.clear();
        maxPageCount = doors.size() / (CHESTSIZE - 9) + ((doors.size() % (CHESTSIZE - 9)) == 0 ? 0 : 1);

        refresh();
    }

    private void refresh()
    {
        if (pageType == PageType.CONFIRMATION || pageType == PageType.DOORINFO)
        {
            fillInfoHeader();
            if (pageType == PageType.CONFIRMATION)
                fillConfirmationItems();
            else
                fillInformationItems();
        }
        else if (pageType == PageType.DOORLIST || pageType == PageType.DOORCREATION)
        {
            fillDefaultHeader();
            fillDoors();
        }

        inventory = Bukkit.createInventory(player, CHESTSIZE, messages.getString(PageType.getMessage(pageType)));
        player.openInventory(inventory);
        items.forEach((k, v) -> inventory.setItem(k, v.getItemStack()));
    }

    private void fillOwnerListHeader()
    {
        fillInfoHeader();

        ArrayList<String> lore = new ArrayList<>();
        if (doorOwnerPage != 0)
        {
            addLore(lore, messages.getString("GUI.ToPage") + doorOwnerPage + messages.getString("GUI.OutOf")
                + maxDoorOwnerPageCount);
            items.put(1, new GUIItem(PAGESWITCHMAT, messages.getString("GUI.PreviousPage"), lore, doorOwnerPage));
            lore.clear();
        }

        if ((doorOwnerPage + 1) < maxDoorOwnerPageCount)
        {
            addLore(lore, messages.getString("GUI.ToPage") + (doorOwnerPage + 2) + messages.getString("GUI.OutOf")
                + maxDoorOwnerPageCount);
            items.put(7, new GUIItem(PAGESWITCHMAT, messages.getString("GUI.NextPage"), lore, doorOwnerPage + 2));
            lore.clear();
        }
    }

    private void fillInfoHeader()
    {
        ArrayList<String> lore = new ArrayList<>();
        items.put(0, new GUIItem(PAGESWITCHMAT, messages.getString("GUI.PreviousPage"), lore, page + 1));
        lore.clear();

        addLore(lore, messages.getString("GUI.MoreInfoMenu") + door.getName());
        addLore(lore, "This door has ID " + door.getDoorUID());
        addLore(lore, messages.getString(DoorType.getNameKey(door.getType())));
        items.put(4, new GUIItem(CURRDOORMAT, door.getName() + ": " + door.getDoorUID(), lore, 1));
    }

    private void fillDefaultHeader()
    {
        ArrayList<String> lore = new ArrayList<>();
        if (page != 0)
        {
            addLore(lore, messages.getString("GUI.ToPage") + page + messages.getString("GUI.OutOf") + maxPageCount);
            items.put(0, new GUIItem(PAGESWITCHMAT, messages.getString("GUI.PreviousPage"), lore, page));
            lore.clear();
        }

        addLore(lore, sortAlphabetically ? messages.getString("GUI.SORTED.Alphabetically") :
            messages.getString("GUI.SORTED.Numerically"));
        items.put(2, new GUIItem(TOGGLEDOORMAT, messages.getString("GUI.SORTED.Change"), lore, 1));
        lore.clear();

        addLore(lore, "§7Cerca una porta in base al suo nome.");
        items.put(1, new GUIItem(SEARCH, "§bCerca", lore, 1));
        lore.clear();

        addCreationBook(DoorType.DRAWBRIDGE, 3, "GUI.NewDrawbridge");
        addCreationBook(DoorType.DOOR, 4, "GUI.NewDoor");
        addCreationBook(DoorType.PORTCULLIS, 5, "GUI.NewPortcullis");
        addCreationBook(DoorType.SLIDINGDOOR, 6, "GUI.NewSlidingDoor");

        if ((page + 1) < maxPageCount)
        {
            addLore(lore,
                    messages.getString("GUI.ToPage") + (page + 2) + messages.getString("GUI.OutOf") + maxPageCount);
            items.put(8, new GUIItem(PAGESWITCHMAT, messages.getString("GUI.NextPage"), lore, page + 2));
            lore.clear();
        }
    }

    private void addCreationBook(DoorType type, int idx, String message)
    {
        if (player.hasPermission(DoorType.getPermission(type)))
        {
            ArrayList<String> lore = new ArrayList<>();
            addLore(lore, messages.getString("GUI.NewObjectLong") + messages.getString(message));
            items.put(idx, new GUIItem(NEWDOORMAT, messages.getString(message), lore, page + 1));
        }
    }

    // Place all the options to (not) confirm door deletion
    private void fillConfirmationItems()
    {
        int mid = (CHESTSIZE - 9) / 2 + 4;
        for (int idx = 9; idx < CHESTSIZE; ++idx)
        {
            ArrayList<String> lore = new ArrayList<>();
            if (idx == mid) // Middle block.
            {
                addLore(lore, messages.getString("GUI.ConfirmDelete"));
                items.put(idx, new GUIItem(NOTCONFIRMMAT, messages.getString("GUI.Confirm"), lore, 1, CONFIRMDATA));
            }
            else
            {
                addLore(lore, messages.getString("GUI.NotConfirm"));
                items.put(idx, new GUIItem(CONFIRMMAT, messages.getString("GUI.No"), lore, 1, NOTCONFIRMDATA));
            }
        }
    }

    // Add all the currently selected door's information items.
    private void fillInformationItems()
    {
        int position = 9;
        for (DoorAttribute attr : DoorType.getAttributes(door.getType()))
        {
            if (!plugin.getCommander().hasPermissionNodeForAction(player, attr))
                continue;

            if (attr == DoorAttribute.NOTIFICATIONS &&
                !plugin.getConfigLoader().allowNotifications())
                continue;

            GUIItem item = getGUIItem(door, attr);
            if (item != null)
                items.put(position++, item);
        }
    }

    // Populate the inventory (starting at the second row, as counted from the top)
    // with doors.
    private void fillDoors()
    {
        int offset = page * (CHESTSIZE - 9);
        int endCount = Math.min((CHESTSIZE - 9), (doors.size() - offset));
        ArrayList<String> lore = new ArrayList<>();
        for (int idx = 0; idx < endCount; ++idx)
        {
            int realIdx = offset + idx;
            DoorType doorType = doors.get(realIdx).getType();
            if (doorType == null)
            {
                plugin.getMyLogger()
                    .logMessage("Failed to determine doorType of door: " + doors.get(realIdx).getDoorUID(), true,
                                false);
                continue;
            }
            addLore(lore, messages.getString("GUI.DoorHasID") + doors.get(realIdx).getDoorUID());
            addLore(lore, messages.getString(DoorType.getNameKey(doorType)));
            GUIItem item = new GUIItem(DOORTYPES[DoorType.getValue(doorType)], doors.get(realIdx).getName(), lore, 1);
            item.setDoor(doors.get(realIdx));
            items.put(idx + 9, item);
            lore.clear();
        }
    }

    private boolean isStillOwner()
    {
        if (door != null &&
            plugin.getCommander().getPermission(player.getUniqueId().toString(), door.getDoorUID()) == -1)
        {
            doors.remove(door);
            door = null;

            pageType = PageType.DOORLIST;
            return false;
        }
        return true;
    }

    public void handleInput(int interactionIDX)
    {
        if (!items.containsKey(interactionIDX))
            return;

        boolean header = Util.between(interactionIDX, 0, 8);

        switch (pageType)
        {
        case CONFIRMATION:
            handleInputConfirmation(interactionIDX);
            break;

        case DOORINFO:
            handleInputDoorInfo(interactionIDX);
            break;

        case DOORLIST:
            handleInputDoorList(interactionIDX, header);
            break;

        case DOORCREATION: // Unimplemented
        default:
            break;
        }
    }

    private void handleInputConfirmation(int interactionIDX)
    {
        if (!isStillOwner())
            return;
        int mid = (CHESTSIZE - 9) / 2 + 4;

        if (interactionIDX == mid)
            deleteDoor();

        pageType = PageType.DOORINFO;
        update();
    }

    private void handleInputDoorInfo(int interactionIDX)
    {
        if (interactionIDX == 0)
        {
            pageType = PageType.DOORLIST;
            update();
        }
        else
        {
            DoorAttribute attribute = items.get(interactionIDX).getDoorAttribute();
            if (attribute == null)
                return;

            if (!plugin.getCommander().hasPermissionForAction(player, door.getDoorUID(), attribute))
            {
                update();
                return;
            }

            switch (attribute)
            {
            case LOCK:
                door.setLock(!door.isLocked());
                plugin.getCommander().setLock(door.getDoorUID(), door.isLocked());
                update();
                break;
            case TOGGLE:
                plugin.getCommandHandler().openDoorCommand(player, door);
                break;
            case INFO:
                plugin.getCommandHandler().listDoorInfo(player, door);
                break;
            case DELETE:
                pageType = PageType.CONFIRMATION;
                update();
                break;
            case RELOCATEPOWERBLOCK:
                plugin.getCommandHandler().startPowerBlockRelocator(player, door);
                close();
                break;
            case DIRECTION_STRAIGHT:
            case DIRECTION_ROTATE:
                changeOpenDir(player, door);
                break;
            case CHANGETIMER:
                plugin.getCommandHandler().startTimerSetter(player, door.getDoorUID());
                close();
                break;
            case BLOCKSTOMOVE:
                plugin.getCommandHandler().startBlocksToMoveSetter(player, door.getDoorUID());
                close();
                break;
            case ADDOWNER:
                plugin.getCommandHandler().startAddOwner(player, door.getDoorUID());
                close();
                break;
            case REMOVEOWNER:
                switchToRemoveOwner();
                break;
            case NOTIFICATIONS:
                boolean newStatus = !door.notificationEnabled();
                door.setNotificationEnabled(newStatus);
                plugin.getCommander().updateDoorNotify(door.getDoorUID(), newStatus);
                update();
                break;
            case BYPASS_PROTECTIONS:
                boolean bypassProtections = !door.bypassProtections();
                door.setBypassProtections(bypassProtections);
                plugin.getCommander().updateDoorBypassProtections(door.getDoorUID(), bypassProtections);
                update();
                break;
            }
        }
    }

    private void handleInputDoorList(int interactionIDX, boolean header)
    {
        if (interactionIDX == 0)
        {
            --page;
            update();
        }
        else if (interactionIDX == 1)
        {
            close();
            BigDoors.get().getInSearch().add(player.getName());
            Util.messagePlayer(player, "§e§lBigDoors §8| §7Invia ora il nome della porta che intendi cercare. " +
                    "In alternativa, digita 'annulla' per annullare questa operazione.");
        }
        else if (interactionIDX == 2)
        {
            sortAlphabetically = !sortAlphabetically;
            sort();
            update();
        }
        else if (interactionIDX == 8)
        {
            ++page;
            update();
        }
        else if (header)
        {
            String itemName = items.get(interactionIDX).getName();
            if (itemName.equals(messages.getString("GUI.NewDoor")))
                startCreationProcess(player, DoorType.DOOR);
            else if (itemName.equals(messages.getString("GUI.NewPortcullis")))
                startCreationProcess(player, DoorType.PORTCULLIS);
            else if (itemName.equals(messages.getString("GUI.NewDrawbridge")))
                startCreationProcess(player, DoorType.DRAWBRIDGE);
            else if (itemName.equals(messages.getString("GUI.NewSlidingDoor")))
                startCreationProcess(player, DoorType.SLIDINGDOOR);
        }
        else
        {
            door = items.get(interactionIDX).getDoor();
            if (door == null)
            {
                Util.messagePlayer(player,
                                   "An unexpected error occurred while trying to open a sub-menu for a door! Try again!");
                close();
                return;
            }
            if (isStillOwner())
                pageType = PageType.DOORINFO;
            update();
        }
    }

    private void switchToRemoveOwner()
    {
        plugin.getCommandHandler().startRemoveOwner(player, door.getDoorUID());
        close();
    }

    private void sort()
    {
        if (sortAlphabetically)
            Collections.sort(doors, Comparator.comparing(Door::getName));
        else
            Collections.sort(doors, Comparator.comparing(Door::getDoorUID));
    }

    private GUIItem getGUIItem(Door door, DoorAttribute attr)
    {
        // If the permission level is higher than the
        if (door.getPermission() > attr.getPermissionLevel())
            return null;

        ArrayList<String> lore = new ArrayList<>();
        String desc, loreStr;
        GUIItem ret = null;

        switch (attr)
        {
        case LOCK:
            if (door.isLocked())
                ret = new GUIItem(LOCKDOORMAT, messages.getString("GUI.UnlockDoor"), null, 1, UNLOCKEDDATA);
            else
                ret = new GUIItem(UNLOCKDOORMAT, messages.getString("GUI.LockDoor"), null, 1, LOCKEDDATA);
            break;

        case TOGGLE:
            desc = messages.getString("GUI.ToggleDoor");
            ret = new GUIItem(TOGGLEDOORMAT, desc, lore, 1);
            break;

        case INFO:
            desc = messages.getString("GUI.GetInfo");
            ret = new GUIItem(INFOMAT, desc, lore, 1);
            break;

        case DELETE:
            desc = messages.getString("GUI.DeleteDoor");
            loreStr = messages.getString("GUI.DeleteDoorLong");
            addLore(lore, loreStr);
            ret = new GUIItem(DELDOORMAT, desc, lore, 1);
            break;

        case RELOCATEPOWERBLOCK:
            desc = messages.getString("GUI.RelocatePowerBlock");
            loreStr = messages.getString("GUI.RelocatePowerBlockLore");
            addLore(lore, loreStr);
            ret = new GUIItem(RELOCATEPBMAT, desc, lore, 1);
            break;

        case CHANGETIMER:
            desc = messages.getString("GUI.ChangeTimer");
            loreStr = door.getAutoClose() > -1 ?
                messages.getString("GUI.ChangeTimerLore") + door.getAutoClose() + "s." :
                messages.getString("GUI.ChangeTimerLoreDisabled");
            addLore(lore, loreStr);
            int count = Math.max(door.getAutoClose(), 1);
            ret = new GUIItem(CHANGETIMEMAT, desc, lore, count);
            break;

        case DIRECTION_STRAIGHT:
            desc = messages.getString("GUI.Direction.Name");
            loreStr = messages.getString("GUI.Direction.ThisDoorGoes")
                + messages.getString(RotateDirection.getNameKey(door.getOpenDir()));
            addLore(lore, loreStr);
            ret = new GUIItem(SETOPENDIRMAT, desc, lore, 1);
            break;

        case DIRECTION_ROTATE:
            desc = messages.getString("GUI.Direction.Name");
            loreStr = messages.getString("GUI.Direction.ThisDoorOpens")
                + messages.getString(RotateDirection.getNameKey(door.getOpenDir()));
            addLore(lore, loreStr);
            addLore(lore,
                    messages.getString("GUI.Direction.Looking") + (door.getType() == DoorType.DOOR ? messages
                        .getString(RotateDirection.getNameKey(RotateDirection.DOWN)) :
                        door.getLookingDir() == DoorDirection.NORTH ?
                            messages.getString(RotateDirection.getNameKey(RotateDirection.EAST)) :
                        messages.getString(RotateDirection.getNameKey(RotateDirection.NORTH))));
            ret = new GUIItem(SETOPENDIRMAT, desc, lore, 1);
            break;

        case BLOCKSTOMOVE:
            desc = messages.getString("GUI.BLOCKSTOMOVE.Name");
            if (door.getBlocksToMove() <= 0)
                loreStr = messages.getString("GUI.BLOCKSTOMOVE.Unavailable");
            else
                loreStr = messages.getString("GUI.BLOCKSTOMOVE.Available") + " " + door.getBlocksToMove();
            addLore(lore, loreStr);
            ret = new GUIItem(SETBTMOVEMAT, desc, lore, 1);
            break;

        case ADDOWNER:
            desc = messages.getString("GUI.ADDOWNER");
            ret = new GUIItem(ADDOWNERMAT, desc, lore, 1, PLAYERHEADDATA);
            break;

        case REMOVEOWNER:
            desc = messages.getString("GUI.REMOVEOWNER");
            ret = new GUIItem(REMOVEOWNERMAT, desc, lore, 1, SKULLDATA);
            break;

        case NOTIFICATIONS:
            desc = messages.getString("GUI.ReceiveNotifications");
            addLore(lore, door.notificationEnabled() ?
                messages.getString("GUI.ReceiveNotificationsLoreEnabled") :
                    messages.getString("GUI.ReceiveNotificationsLoreDisabled")
                );
            ret = new GUIItem(door.notificationEnabled() ? NOTIFICATIONSMAT_ON : NOTIFICATIONSMAT_OFF, desc, lore, 1);
            break;

        case BYPASS_PROTECTIONS:
            desc = messages.getString("GUI.BypassProtections");
            addLore(lore, door.bypassProtections() ?
                messages.getString("GUI.BypassProtectionsLoreEnabled") :
                    messages.getString("GUI.BypassProtectionsLoreDisabled")
                );
            ret = new GUIItem(door.bypassProtections() ? BYPASS_PROTECTIONS_ON : BYPASS_PROTECTIONS_OFF, desc, lore, 1);
            break;

        }

        if (ret != null)
            ret.setDoorAttribute(attr);
        return ret;
    }

    public Player getPlayer()
    {
        return player;
    }

    public void close()
    {
        player.closeInventory();
        plugin.removeGUIUser(this);
    }

    /*
     * Implementation of all the that require additional actions not provided by the
     * commander.
     */

    private void deleteDoor()
    {
        if (plugin.getCommander().removeDoor(getPlayer(), door.getDoorUID()))
            doors.remove(door);
    }

    private void startCreationProcess(Player player, DoorType type)
    {
        player.closeInventory();
        plugin.getCommandHandler().startCreator(player, null, type);
    }

    // Changes the opening direction for a door.
    private void changeOpenDir(Player player, Door door)
    {
        RotateDirection curOpenDir = door.getOpenDir();
        RotateDirection newOpenDir;

        if (door.getType() == DoorType.SLIDINGDOOR)
            newOpenDir = curOpenDir == RotateDirection.NONE ? RotateDirection.NORTH :
                curOpenDir == RotateDirection.NORTH ? RotateDirection.EAST :
                curOpenDir == RotateDirection.EAST ? RotateDirection.SOUTH :
                curOpenDir == RotateDirection.SOUTH ? RotateDirection.WEST : RotateDirection.NORTH;
        else if (door.getType() == DoorType.DRAWBRIDGE)
            newOpenDir = curOpenDir == RotateDirection.NONE ? RotateDirection.NORTH :
                curOpenDir == RotateDirection.NORTH ? RotateDirection.SOUTH :
                curOpenDir == RotateDirection.SOUTH ? RotateDirection.NORTH :
                curOpenDir == RotateDirection.EAST ? RotateDirection.WEST : RotateDirection.EAST;
        else if (door.getType() == DoorType.PORTCULLIS)
            // Switch between UP and DOWN. When invalid, default to DOWN.
            newOpenDir = curOpenDir == RotateDirection.DOWN ? RotateDirection.UP : RotateDirection.DOWN;
        else
            newOpenDir = curOpenDir == RotateDirection.NONE ? RotateDirection.CLOCKWISE :
                curOpenDir == RotateDirection.CLOCKWISE ? RotateDirection.COUNTERCLOCKWISE : RotateDirection.CLOCKWISE;

        door.setOpenDir(newOpenDir);
        plugin.getCommander().updateDoorOpenDirection(door.getDoorUID(), newOpenDir);
        refresh();
    }
}
