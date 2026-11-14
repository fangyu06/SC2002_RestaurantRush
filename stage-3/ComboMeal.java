// A combo is made of two existing menu items. It IS a MenuItem (inheritance),
// so the menu, Order and chooseItem accept it unchanged, and it HAS two
// MenuItems (composition of existing items).
// It overrides getPriceCents and getUnits (polymorphism):
//   preparation units = the sum of both parts;
//   price = 90% of the parts' combined price, rounded to the nearest cent,
//   e.g. Salad + Pasta = ($9.00 + $18.00) x 0.9 = $24.30, 3 units.
public class ComboMeal extends MenuItem{
    public static final double COMBO_MULTIPLIER=0.9;

    private final MenuItem first;
    private final MenuItem second;

    public ComboMeal(MenuItem first, MenuItem second){
        super(nameOf(first, second), first.getPriceCents()+second.getPriceCents(), first.getUnits()+second.getUnits());
        this.first=first;
        this.second=second;
    }

    // Checked before super() runs, so a missing part gives a clear message.
    private static String nameOf(MenuItem first, MenuItem second){
        if (first==null||second==null){
            throw new IllegalArgumentException("A combo needs two menu items.");
        }
        return first.getName()+" + "+second.getName();
    }

    @Override
    public int getPriceCents(){
        int combinedPrice=first.getPriceCents()+second.getPriceCents();
        return (int) Math.round(combinedPrice*COMBO_MULTIPLIER);
    }
    @Override
    public int getUnits(){
        return first.getUnits()+second.getUnits();
    }
}
