package dev.infernity.rollplayer.components;

import dev.infernity.rollplayer.util.IdentifierUtil;
import net.dv8tion.jda.api.components.actionrow.ActionRow;
import net.dv8tion.jda.api.components.buttons.Button;
import net.dv8tion.jda.api.components.container.Container;
import net.dv8tion.jda.api.components.container.ContainerChildComponent;
import net.dv8tion.jda.api.components.selections.StringSelectMenu;
import net.dv8tion.jda.api.components.separator.Separator;
import net.dv8tion.jda.api.components.textdisplay.TextDisplay;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import org.jetbrains.annotations.Nullable;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class PaginationComponent {
    public enum Result{
        OK,
        PAGE_DOES_NOT_EXIST,
        NOT_YOUR_MENU,
        EXPIRED
    }

    public record Page(String name, List<ContainerChildComponent> content){
        public Page(String name, String content){
            this(name, List.of(TextDisplay.of(content)));
        }
    }

    public final List<Page> pages;
    public final int pageLength;
    public final UUID uuid;
    public long expiresAt;
    public int page;
    public boolean inStringSelectMenu;
    public final long menuOpener;
    public final String header;

    public PaginationComponent(List<Page> pages, User menuOpener, @Nullable String header) {
        this.pages = pages;
        this.pageLength = pages.size();
        this.uuid = UUID.randomUUID();
        this.expiresAt = Instant.now().getEpochSecond() + 900;
        this.menuOpener = menuOpener.getIdLong();
        this.header = Objects.nonNull(header) ? header : "### %s";
    }

    private int previousPage(){
        if (page == 0){
            return pageLength-1;
        }
        return page-1;
    }

    private int nextPage(){
        if (page == pageLength-1){
            return 0;
        }
        return page+1;
    }

    public Container asContainer(){
        if (inStringSelectMenu){
            if (pageLength > 25) throw new IllegalStateException();
            ArrayList<ContainerChildComponent> pageContent = new ArrayList<>();

            pageContent.add(TextDisplay.ofFormat(header, "Menu"));
            pageContent.add(Separator.createInvisible(Separator.Spacing.SMALL));

            pageContent.add(TextDisplay.of("**Pick a page to open**"));
            var menu = StringSelectMenu.create(IdentifierUtil.identifier("pagination", "%s:menu", uuid.toString()));
            for (int i = 0; i < pageLength; i++) {
                var p = pages.get(i);
                menu.addOption(p.name(), IdentifierUtil.identifier("pagination", "%s:toPage:%d:sel", uuid.toString(), i));
            }
            menu.setDefaultValues(IdentifierUtil.identifier("pagination", "%s:toPage:%d:sel", uuid.toString(), page));
            menu.setMinValues(1);
            menu.setMaxValues(1);
            pageContent.add(ActionRow.of(menu.build()));
            pageContent.add(ActionRow.of(Button.danger(IdentifierUtil.identifier("pagination", "%s:exitMenu", uuid.toString()), "Back")));
            return Container.of(pageContent);
        }
        var pageObject = this.pages.get(page);
        var pageContent = new ArrayList<>(pageObject.content);

        pageContent.addFirst(Separator.createInvisible(Separator.Spacing.SMALL));
        pageContent.addFirst(TextDisplay.ofFormat(header, pageObject.name));
        pageContent.add(Separator.createInvisible(Separator.Spacing.SMALL));

        if (pageLength <= 25) {
            pageContent.add(ActionRow.of(
                    Button.primary(IdentifierUtil.identifier("pagination", "%s:toPage:0:bf", uuid.toString()), Emoji.fromUnicode("⏪")),
                    Button.primary(IdentifierUtil.identifier("pagination", "%s:toPage:%d:bp", uuid.toString(), previousPage()), Emoji.fromUnicode("◀")),
                    Button.primary(IdentifierUtil.identifier("pagination", "%s:goToMenu", uuid.toString(), previousPage()), Emoji.fromUnicode("\uD83E\uDDED")),
                    Button.primary(IdentifierUtil.identifier("pagination", "%s:toPage:%d:bn", uuid.toString(), nextPage()), Emoji.fromUnicode("▶")),
                    Button.primary(IdentifierUtil.identifier("pagination", "%s:toPage:%d:bl", uuid.toString(), pageLength - 1), Emoji.fromUnicode("⏩"))
            ));
        } else {
            pageContent.add(ActionRow.of(
                    Button.primary(IdentifierUtil.identifier("pagination", "%s:toPage:0:bf", uuid.toString()), Emoji.fromUnicode("⏪")),
                    Button.primary(IdentifierUtil.identifier("pagination", "%s:toPage:%d:bp", uuid.toString(), previousPage()), Emoji.fromUnicode("◀")),
                    Button.primary(IdentifierUtil.identifier("pagination", "%s:toPage:%d:bn", uuid.toString(), nextPage()), Emoji.fromUnicode("▶")),
                    Button.primary(IdentifierUtil.identifier("pagination", "%s:toPage:%d:bl", uuid.toString(), pageLength - 1), Emoji.fromUnicode("⏩"))
            ));
        }
        return Container.of(pageContent);
    }

    public Result toPage(int page){
        if (page > pageLength){
            return Result.PAGE_DOES_NOT_EXIST;
        }
        if (Instant.now().getEpochSecond() > this.expiresAt){
            return Result.EXPIRED;
        }
        this.page = page;
        this.expiresAt = Instant.now().getEpochSecond() + 900;
        return Result.OK;
    }

    public Result goToMenu(){
        if (Instant.now().getEpochSecond() > this.expiresAt){
            return Result.EXPIRED;
        }
        inStringSelectMenu = true;
        return Result.OK;
    }

    public Result exitMenu(){
        if (Instant.now().getEpochSecond() > this.expiresAt){
            return Result.EXPIRED;
        }
        inStringSelectMenu = false;
        return Result.OK;
    }
}
