// One dish on the menu.
// Money is stored as integer cents (900 means $9.00) so that we never get
// rounding errors from decimal numbers. The CLI turns cents into dollars
// only when it displays them.
public class MenuItem{
    private final String name;
    private final int priceCents;
    private final int preparationUnits; // how many Chef turns the dish needs

    public MenuItem(String name, int priceCents, int preparationUnits){
        if (name==null||name.isBlank()){
            throw new IllegalArgumentException("A menu item needs a name.");
        }
        if (priceCents<=0){
            throw new IllegalArgumentException("The price of "+name+" must be more than 0 cents.");
        }
        if (preparationUnits<1){
            throw new IllegalArgumentException(name+" needs at least 1 preparation unit.");
        }
        this.name=name;
        this.priceCents=priceCents;
        this.preparationUnits=preparationUnits;
    }
    public String getName(){
        return this.name;
    }
    public int getPriceCents(){
        return this.priceCents;
    }
    public int getUnits(){
        return this.preparationUnits;
    }
}
