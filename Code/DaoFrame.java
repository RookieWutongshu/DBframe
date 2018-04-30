package cn.wwt.frame;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import com.mysql.jdbc.Connection;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;



public class DaoFrame {
    private static DaoFrame instance;
    private Connection connec;
    private PreparedStatement ps;
    private ResultSet rs;
    String dbName=null;
    String userName=null;
    String password=null;
    static StringBuilder sb=null;

    public static synchronized DaoFrame getInstance() throws Exception{
        if (instance==null) {
            instance=new DaoFrame();
        }
        return instance;
    }
    private DaoFrame() throws Exception {
        getDB();
    }
    private void getDB() throws Exception {
        try {
            SAXReader saxReader=new SAXReader();
            Document doc=saxReader.read(new File("src/dbconfig.xml"));
            Element root=doc.getRootElement();
            List<Element> list=root.elements();
            for(Element e:list) {
                dbName=e.element("DBName").getText();
                userName=e.element("user").getText();
                password=e.element("password").getText();
            }
            Class.forName("com.mysql.jdbc.Driver");
            sb=new StringBuilder();
            sb.append("jdbc:mysql://localhost:3306/");
            sb.append(dbName);
            sb.append("?useUnicode=true&characterEncoding=utf-8&useSSL=yes");


        } catch (DocumentException e) {
            // TODO: handle exception
            throw new Exception("dbconfig读取出错，请检查正确是否放置于src目录下"+e.getMessage());

        } catch (ClassNotFoundException e1) {
            // TODO Auto-generated catch block
            throw new Exception("驱动加载失败");
        }catch(NullPointerException e){
            throw new Exception("请检查你的xml文件书写格式是否规范,可以参考xmlExample里要求的书写格式");
        }
        catch(Exception e){
            System.out.println(e.getMessage()+"hhhhh");
        }

    }
    public Connection getConnec() throws Exception {
        try{
        connec=(Connection) DriverManager.getConnection(sb.toString(),userName,password);
//        System.out.println("sql语句为："+sb.toString()+","+userName+","+password);
            return connec;
        } catch (Exception e){
            throw new Exception("在getConnec出错："+e.getMessage());
        }

    }
    public void finalize() throws SQLException {
        if (connec != null) {
            connec.close();
        }
        if (ps != null) {
            ps.close();
        }
        if (rs != null) {
            rs.close();
        }
    }
    public void testObj(Object bi) throws Exception {
        /*TODO 现在1.0先只判断一个注解，之后要用list集合判断，因为有些类可能不只使用一个注解。最后还要能够通过写注解
        TODO 或写配置文件，写一个即可。刚刚知道原来注解的值与类中已有的属性名一样的话就不用特地获取注解来取值，直接用get方法即可，
        TODO 等以后优化
        */
        if(!(bi instanceof BaseInterfaceClass)){
            throw new Exception("请确保该Bean类继承了BaseInterfaceClass类且不为空");
        }
        Class biClass= bi.getClass();
        if(!biClass.isAnnotationPresent(DBProperty.class)){
            throw new Exception("请为Bean类写InitValue注解");
        }
        DBProperty annotation= (DBProperty) biClass.getAnnotation(DBProperty.class);
        if(annotation.PRIMARY_KEY_NAME()==null||"".equals(annotation.PRIMARY_KEY_NAME())){
            throw new Exception("该类未设置主键");
        }
        if(annotation.TABLE_NAME()==null||"".equals(annotation.TABLE_NAME())){
            throw new Exception("该类未设置对应的表名");
        }

    }


    /**
     *this method was used to insert a javabean object to database
     * @param bi a obj that extends BaseInterfaceClass
     * @return the line number that were effected
     * @throws Exception SQLException illegalAccessException
     */
    public int add(Object bi) throws Exception {
        testObj(bi);
        Field[] fields=bi.getClass().getDeclaredFields();
        System.out.println("fields的长度："+fields.length);
		StringBuilder bs=new StringBuilder();
		DBProperty anntation=bi.getClass().getAnnotation(DBProperty.class);
        String primaryKey=anntation.PRIMARY_KEY_NAME();
		int primaryKeyIndex=0,result;//放主键在反射中的索引
		bs.append("insert into ");
		bs.append(anntation.TABLE_NAME()+"(");
        /* TODO ：把用String的equals方法改成hashcode比较似乎会快点，后面尝试一下*/
        for(int i=0;i<fields.length-1;i++){
            if(fields[i].getName().equals(primaryKey)){
                primaryKeyIndex=i;
            }
            else{
                bs.append(fields[i].getName()+",");
            }
        }
        if(primaryKeyIndex==0){
            primaryKeyIndex=fields.length-1;
            bs.deleteCharAt(bs.length()-1);
        }else{
            bs.append(fields[fields.length-1].getName());
        }
        //TODO:现在的是主键由数据库给予而不是由人为的设置，以后改成由用户选择是否由数据库给主键值还是用户可以自己设置
        bs.append(") values(");
        for(int i=0;i<fields.length-1;i++){
            if(i==primaryKeyIndex){
                bs.deleteCharAt(bs.length()-1);
            }else{
                bs.append("?,");
            }
        }
        bs.deleteCharAt(bs.length()-1);
        bs.append(")");
        try{
            ps=getConnec().prepareStatement(bs.toString());
            for(int i=0;i<fields.length-1;i++){
                if(i==primaryKeyIndex){
                    continue;
                }
                fields[i].setAccessible(true);
//                if (!fields[i].getGenericType().toString().equals(fields[i].get(bi).getClass().toString())) {
//                    throw new Exception(fields[i].getName()+"属性的类型为："+fields[i].getGenericType().toString()+
//                            "与设置的值的类型"+fields[i].get(bi).getClass().toString()+"不同");
//                }
                /*Todo:弄出一个能比较数据库表属性数据类型和Bean类属性数据类型的判断*/
                ps.setObject(i+1,fields[i].get(bi));
            }
            result=ps.executeUpdate();
        }catch (SQLException e){
            throw new Exception("执行sql语句时出错，语句为:"+bs.toString()+"错误信息："+e.getMessage());
        }
        catch (Exception e){
            throw new Exception("奇葩错误："+e.getMessage());
        }
        finalize();

        return result;//实际返回的不是主键索引，而是执行结果，只是懒的再弄个变量名了
	}

    /**
     * you can use this method to get a Object list query by an Element
     * @param beanClass
     * @param attribute
     * @param value
     * @return a object list for result
     * @throws IllegalAccessException,ClassCastException, SQLException,SecurityException,Exception
     */

	public List<Object> selectByElement (Class beanClass,String attribute,String value) throws Exception {
        StringBuilder bs=new StringBuilder();
        String methodName="";
            try {
                Class obj=beanClass;
                testObj(obj.newInstance());
                DBProperty anntation= (DBProperty) obj.getAnnotation(DBProperty.class);
                Field[] fields= obj.getDeclaredFields();
                List<Method>methods = new ArrayList<Method>();
                List<String>attributeNames =new ArrayList<String>();
                List<Object> resultList=new ArrayList<Object>();
                /*TODO 可以尝试用intern方法或别的方法，总觉得优化的很差*/
                for(int i=0;i<fields.length;i++){
                    fields[i].setAccessible(true);
                    String attributeName=fields[i].getName();
                    methodName="set"+attributeName.substring(0,1).toUpperCase()
                            +attributeName.substring(1);
                    Method method=obj.getMethod(methodName, (Class)fields[i].getGenericType());
                    attributeNames.add(attributeName);
                    methods.add(method);
                }
                bs.append("select * from ");
                bs.append(anntation.TABLE_NAME());
                bs.append(" where "+ attribute +"=?");
                ps=getConnec().prepareStatement(bs.toString());
                ps.setObject(1,value);
                rs=ps.executeQuery();
                System.out.println(1);
                while(rs.next()){
                    Object newBean = beanClass.newInstance();
                    for(int i=0;i<methods.size();i++){
                        methods.get(i).invoke(newBean, rs.getObject(i+1));
                    }
                    resultList.add(newBean);
                }
                finalize();
                return resultList;
            }
//TODO:写一个能判断插入数据库里的属性名和Bean类里属性名，属性类型是否一致。注意varchar等于String！
            catch(IllegalAccessException e){
                throw new Exception("请确保你的Bean类有一个默认构造函数");
            }
            catch(ClassCastException e){
                throw new Exception("请确保你的Bean类继承了BaseInterfaseClass");
            }
            catch(SQLException e){
                throw new Exception("在执行sql语句时出错,语句为"+bs.toString());

            }
            catch (SecurityException e){
                throw new Exception("Bean类没有此方法："+methodName);
            }
            catch (Exception e){
                throw new Exception("奇葩错误："+e.getMessage());
            }


    }

    /**
     * you can update the database by give it a Bean object
     * @param bi
     * @return a number means how many lines that database was influence
     * @throws Exception
     */

	public int update(Object bi) throws Exception {
        testObj(bi);
        StringBuilder bs=new StringBuilder();
        try {
            Class obj = bi.getClass();
            DBProperty anntation = (DBProperty) obj.getAnnotation(DBProperty.class);
            String primaryKey =anntation.PRIMARY_KEY_NAME();
            Field[] fields = obj.getDeclaredFields();
            bs.append("update ");
            bs.append(anntation.TABLE_NAME() + " set ");
            for (int i = 0; i < fields.length; i++) {
                fields[i].setAccessible(true);
                bs.append(fields[i].getName() + "=?,");
            }
            bs.deleteCharAt(bs.length()-1);
            bs.append(" where ");
            bs.append(primaryKey+"=?");
            ps=getConnec().prepareStatement(bs.toString());
            for(int i=0;i<fields.length;i++){
                fields[i].setAccessible(true);
                ps.setObject(i+1,fields[i].get(bi));
            }
            Field primarykeyfield=obj.getDeclaredField(primaryKey);
            primarykeyfield.setAccessible(true);
            ps.setObject(fields.length+1,primarykeyfield.get(bi));
            int result=ps.executeUpdate();
            finalize();
            return result;

/*TODO:这里准备加一个额外的传判定条件的方法如where id=?&&password=?之类的，或许要用到线程来延时操作或者什么全局变量之类的。Hibernate似乎是直接让人写sql语句？*/
        }catch(SQLException e){
            throw new Exception("sql语句出错，为："+bs.toString()+"错误信息为："+e.getMessage());
        }
        catch (Exception e){
            throw new Exception("奇葩错误："+e.getMessage());
        }
    }

    /**
     * you can use this method to delete a bean object from database
     * @param bi
     * @return a number means how many lines that database was influence
     * @throws SQLException,Exception
     */
    public int delete(Object bi) throws Exception{
	    testObj(bi);
        StringBuilder bs=new StringBuilder();
        try{
            Class obj=bi.getClass();
            DBProperty anntation = (DBProperty) obj.getAnnotation(DBProperty.class);
            Field field=obj.getDeclaredField(anntation.PRIMARY_KEY_NAME());
            field.setAccessible(true);
            bs.append("delete from "+anntation.TABLE_NAME());
            bs.append(" where "+anntation.PRIMARY_KEY_NAME() +"=?");
            ps=getConnec().prepareStatement(bs.toString());
            ps.setObject(1,field.get(bi));
            int result = ps.executeUpdate();
            finalize();
            return result;

        }catch (SQLException e){
            throw new Exception("sql语句执行出错，为："+bs.toString()+"错误信息为："+e.getMessage());
        }
        catch (Exception e){
            throw new Exception("奇葩错误："+e.getMessage());
        }

    }




}
