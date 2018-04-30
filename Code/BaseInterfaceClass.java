package cn.wwt.frame;


abstract public class BaseInterfaceClass implements BaseInterface{

    protected String TABLE_NAME;
    protected String PRIMARY_KEY_NAME;

    public String getTABLE_NAME() {
        return TABLE_NAME;
    }

    public void setTABLE_NAME(String TABLE_NAME) {
        this.TABLE_NAME = TABLE_NAME;
    }

    public String getPRIMARY_KEY_NAME() {
        return PRIMARY_KEY_NAME;
    }

    public void setPRIMARY_KEY_NAME(String pRIMARY_KEY_NAME) {
        PRIMARY_KEY_NAME = pRIMARY_KEY_NAME;
    }


}
