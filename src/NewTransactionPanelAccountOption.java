public class NewTransactionPanelAccountOption {
    public int id;
    public String name;

    public NewTransactionPanelAccountOption(int id, String name) {
        this.id = id;
        this.name = name;
    }

    @Override
    public String toString() {
        return name;
    }
}
