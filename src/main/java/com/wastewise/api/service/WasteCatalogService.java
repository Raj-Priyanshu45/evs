package com.wastewise.api.service;

import com.wastewise.api.model.WasteCategory;
import com.wastewise.api.model.WasteItem;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

@Service
public class WasteCatalogService {
    private final List<WasteCategory> categories = List.of(
            new WasteCategory("all", "All items", "Browse the complete starter guide."),
            new WasteCategory("recyclables", "Recyclables", "Materials commonly accepted for recycling."),
            new WasteCategory("organic", "Organic", "Food and garden waste that can be composted."),
            new WasteCategory("e-waste", "E-waste", "Electronics and devices that need specialist collection."),
            new WasteCategory("hazardous", "Hazardous", "Items that need careful, separate handling."),
            new WasteCategory("textiles", "Textiles", "Wearables and fabric that can often be reused.")
    );

    private final List<WasteItem> items = List.of(
            new WasteItem("used-battery", "Used battery", "Small batteries can contain chemicals and metals that should not enter household bins.", "Hazardous waste", "hazardous", "🔋", "Special drop-off", "Authorized collection", List.of("Tape the terminals of loose lithium batteries where practical.", "Keep batteries dry and away from heat.", "Take them to an authorized battery or e-waste collection point."), List.of("Do not put batteries in regular trash or mixed recycling.", "Do not crush, puncture, or burn a battery."), "If a battery is swollen, leaking, or hot, avoid handling it and contact your local waste service."),
            new WasteItem("plastic-bottle", "Plastic bottle", "Empty beverage and household bottles are often accepted in recycling programs.", "Recyclables", "recyclables", "🧴", "Often recyclable", "Recycling bin", List.of("Empty the bottle and give it a quick rinse if needed.", "Replace the cap if your local program accepts capped bottles.", "Place it in the correct recycling bin."), List.of("Do not place a bottle with liquid still inside.", "Do not assume every type of plastic is accepted locally."), "Check the resin code and your local collection rules; acceptance varies."),
            new WasteItem("cardboard", "Cardboard box", "Clean cardboard boxes are useful paper-fibre recycling material.", "Recyclables", "recyclables", "📦", "Often recyclable", "Recycling bin", List.of("Remove food, plastic wrap, and packing foam.", "Flatten the box to save space.", "Keep it dry and place it with paper recycling."), List.of("Do not recycle greasy or heavily food-soiled cardboard with clean paper."), "Reuse shipping boxes before recycling them."),
            new WasteItem("paper", "Office paper", "Clean, dry paper can usually be collected with paper products.", "Recyclables", "recyclables", "📄", "Often recyclable", "Paper recycling", List.of("Keep paper dry and free of food or oil.", "Remove non-paper bindings where required.", "Place it in the paper recycling stream."), List.of("Do not put wet or oil-soaked paper with clean paper recycling."), "Shredded paper rules vary by recycler, so check before bagging it."),
            new WasteItem("glass-bottle", "Glass bottle", "Glass bottles may be recyclable, but collection systems differ by location.", "Recyclables", "recyclables", "🍾", "Location-dependent", "Glass collection", List.of("Empty the bottle and rinse it if needed.", "Separate it by colour only if your local service asks you to.", "Use a designated glass collection point or bin."), List.of("Do not put broken glass loose into a bag where it can injure someone."), "Wrap broken glass securely and label it before following local disposal guidance."),
            new WasteItem("food-scraps", "Food scraps", "Fruit and vegetable scraps can often be composted instead of sent to landfill.", "Organic", "organic", "🥬", "Compostable", "Compost / wet waste", List.of("Collect scraps in a covered container.", "Use your local wet-waste collection or a suitable compost system.", "Keep contaminants such as plastic wrappers out."), List.of("Do not add plastic packaging to compost.", "Meat, dairy, and cooked food are not accepted by every home compost system."), "A separate kitchen caddy makes everyday sorting easier."),
            new WasteItem("coffee-grounds", "Coffee grounds", "Spent coffee grounds and paper filters are suitable for many compost systems.", "Organic", "organic", "☕", "Usually compostable", "Compost / wet waste", List.of("Let hot grounds cool.", "Empty grounds and paper filters into the organic-waste container.", "Follow your local wet-waste collection rules."), List.of("Do not include plastic pods or foil lids in organic waste."), "Coffee grounds can be added in moderation to a balanced compost mix."),
            new WasteItem("old-phone", "Old mobile phone", "Phones contain recoverable materials and batteries that need specialist handling.", "E-waste", "e-waste", "📱", "Specialist recycling", "E-waste collection", List.of("Back up and erase personal data if the device still works.", "Remove SIM and memory cards.", "Take the phone to an authorized e-waste collection point or take-back program."), List.of("Do not put electronics in household trash.", "Do not remove or damage a built-in battery."), "Use a manufacturer or authorized recycler take-back program where available."),
            new WasteItem("light-bulb", "CFL / fluorescent bulb", "Some bulbs contain hazardous components and should be handled carefully.", "Hazardous waste", "hazardous", "💡", "Special drop-off", "Special collection", List.of("Keep the bulb intact and in its original packaging if possible.", "Take it to a designated lamp or hazardous-waste collection point.", "If it breaks, ventilate the area and follow local cleanup guidance."), List.of("Do not crush the bulb.", "Do not place fluorescent/CFL bulbs in regular trash unless local rules explicitly allow it."), "LED bulbs have different handling rules, but e-waste collection is still a good option."),
            new WasteItem("expired-medicine", "Expired medicine", "Medicines should not automatically go into household waste or drains.", "Hazardous waste", "hazardous", "💊", "Take-back preferred", "Pharmacy / take-back", List.of("Keep medicines in their packaging where possible.", "Ask a pharmacy or local health authority about medicine take-back.", "Follow the product label and local guidance for any medicine not accepted by take-back."), List.of("Do not flush medicines unless the label or local authority specifically instructs you to."), "Keep medicines away from children and pets until safely handed over."),
            new WasteItem("clothing", "Old clothing", "Wearable clothing can often be reused before it is recycled.", "Textiles", "textiles", "👕", "Reuse first", "Donation / textile collection", List.of("Check whether the item is clean and wearable.", "Donate or resell items in good condition.", "Use a textile collection program for worn-out fabric where available."), List.of("Do not donate wet, mouldy, or heavily soiled clothing."), "Repairing or reusing a garment usually keeps more value than recycling it."),
            new WasteItem("cooking-oil", "Used cooking oil", "Pouring cooking oil into drains can cause blockages and environmental harm.", "Hazardous waste", "hazardous", "🫗", "Special handling", "Oil collection / local guidance", List.of("Let the oil cool completely.", "Store small amounts in a sealed, leak-proof container.", "Check local household-waste or oil collection guidance."), List.of("Do not pour oil into sinks, storm drains, or onto soil.", "Do not mix used oil with other chemicals."), "Restaurants and larger quantities may have dedicated used-oil collection services.")
    );

    public List<WasteCategory> getCategories() {
        return categories;
    }

    public List<WasteItem> search(String query, String category) {
        String q = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
        String selectedCategory = category == null ? "all" : category.trim().toLowerCase(Locale.ROOT);
        return items.stream()
                .filter(item -> selectedCategory.isBlank() || selectedCategory.equals("all") || item.categorySlug().equals(selectedCategory))
                .filter(item -> q.isBlank() || searchableText(item).contains(q))
                .toList();
    }

    public Optional<WasteItem> findById(String id) {
        return items.stream().filter(item -> item.id().equalsIgnoreCase(id)).findFirst();
    }

    private String searchableText(WasteItem item) {
        return (item.name() + " " + item.description() + " " + item.category() + " " + item.recyclability() + " " + item.disposalType())
                .toLowerCase(Locale.ROOT);
    }
}
