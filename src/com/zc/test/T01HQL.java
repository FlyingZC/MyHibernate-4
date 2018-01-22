package com.zc.test;

import java.util.Arrays;
import java.util.List;

import org.hibernate.Criteria;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.cfg.Configuration;
import org.hibernate.criterion.Conjunction;
import org.hibernate.criterion.Disjunction;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.service.ServiceRegistryBuilder;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.zc.entity.Department;
import com.zc.entity.Employee;

public class T01HQL
{
    private static SessionFactory sessionFactory;

    private static Session session;

    private static Transaction transaction;

    @Before
    public void init()
    {
        Configuration configuration = new Configuration().configure();

        ServiceRegistry serviceRegistry = new ServiceRegistryBuilder().applySettings(
                configuration.getProperties()).buildServiceRegistry();

        sessionFactory = configuration.buildSessionFactory(serviceRegistry);
        session = sessionFactory.openSession();
        transaction = session.beginTransaction();
    }

    @After
    public void destroy()
    {
        transaction.commit();
        session.close();
        sessionFactory.close();
    }

    @Test
    public void testHQL()
    {//使用?问号 作为占位符
        //1.创建Query对象
        String hql = "FROM Employee e where e.salary>? and e.email LIKE ?";
        Query query = session.createQuery(hql);
        //2.基于位置参数,绑定参数,填充占位符.salary>6000,email like '%a%'
        query.setFloat(0, 6000).setString(1, "%a%");
        //
        List<Employee> emps = query.list();
        System.out.println(emps.size());
    }

    @Test
    public void testHQLNamedParameter()
    {//使用:xxx作为占位符
        //1.创建Query对象
        String hql = "FROM Employee e where e.salary>:sal and e.email LIKE :email and e.dept=:dept";
        Query query = session.createQuery(hql);
        Department dept=new Department();
        dept.setId(1);
        //2.绑定命名参数参数,即:sal对应6000,:email对应%%,填充站位符时,还可以使用!!!setEntity设置一个实体类型
        query.setFloat("sal", 6000).setString("email", "%%").setEntity("dept", dept);
        //
        List<Employee> emps = query.list();
        System.out.println(emps.size());
    }

    //分页查询
    @Test
    public void testPageQuery()
    {
        String hql = "FROM Employee";
        Query query = session.createQuery(hql);
        //查询第三页,每页显示5条记录
        int pageNo = 3;
        int pageSize = 5;
        //第一条记录从0开始.start,size
        List<Employee> emps = query.setFirstResult((pageNo - 1) * pageSize).setMaxResults(pageSize).list();
        for (Employee e : emps)
        {
            System.out.println(e);
        }
    }

    //将查询语句写在.hbm.xml里的<query>标签里 <query name="salaryEmps">
    @Test
    public void testNamedQuery()
    {
        //.hbm.xml里配置的query名
        Query query = session.getNamedQuery("salaryEmps");
        List<Employee> emps = query.setFloat("minSal", 500).setFloat("maxSal", 10000).list();
        for (Employee e : emps)
        {
            System.out.println(e);
        }
    }

    //投影查询: 查询结果仅包含实体的部分属性
    //1.返回一个List<Object[]>
    @Test
    public void testFieldQuery()
    {
        String hql = "SELECT e.email,e.salary FROM Employee e WHERE e.dept=:dept";
        Query query = session.createQuery(hql);
        Department dept = new Department();
        dept.setId(1);
        //每一行记录为一个Object[]
        List<Object[]> result = query.setEntity("dept", dept).list();
        for (Object[] objs : result)
        {
            System.out.println(Arrays.asList(objs));
        }
    }

    //投影查询,只查询部分属性
    //2.返回一个List<Employee>
    @Test
    public void testFieldQuery2()
    {
        //只查询部分属性,需要有对应的构造方法,每一条数据对应一个数组.最后返回List<Object[]>
        String hql = "SELECT new Employee(e.email,e.salary,e.dept) " 
                + " FROM Employee e WHERE e.dept=:dept";
        Query query = session.createQuery(hql);
        Department dept = new Department();
        dept.setId(1);
        //返回一个List<Employee>
        List<Employee> result = query.setEntity("dept", dept).list();
        for (Employee e : result)
        {
            System.out.println(e.getId() + e.getEmail() + e.getSalary() + e.getDept());
        }
    }

    //分组查询,min,max,group by,having
    @Test
    public void testGroupBy()
    {
        String hql = "select min(e.salary),max(e.salary) " 
                    + "from Employee e " + "group by e.dept "
                    + "HAVING min(salary)>:minSal";
        Query query = session.createQuery(hql).setFloat("minSal", 3000);
        List<Object[]> result = query.list();
        for (Object[] obj : result)
        {
            System.out.println(Arrays.asList(obj));
        }
    }

    //!!!HQL迫切左外连接(也可以使用inner join等),返回所有满足条件的记录和左表中不满足条件的记录(所以会有重复记录),返回List<Department>,建议使用这种
    @Test
    public void testLeftJoinFetch()
    {
        //使用distinct去重,或查询后使用HashSet去重
        String hql = "SELECT DISTINCT d FROM Department d LEFT JOIN FETCH d.emps";
        Query query = session.createQuery(hql);
        List<Department> depts = query.list();
        System.out.println(depts.size());
    }

    //左外连接,返回素有满足条件的List<Object[]>,注意返回类型与fetch迫切做外连接的区别,不推荐使用
    @Test
    public void testLeftJoin()
    {
        String hql = "FROM Department d LEFT JOIN d.emps";
        Query query = session.createQuery(hql);
        List<Object[]> result = query.list();
        System.out.println(result);
        for (Object[] obj : result)
        {
            System.out.println(Arrays.asList(obj));
        }
    }

  //左外连接,返回素有满足条件的List<Object[]>,注意返回类型与fetch迫切做外连接的区别,不推荐使用
    @Test
    public void testLeftJoin2()
    {
        String hql = "SELECT DISTINCT d FROM Department d LEFT JOIN d.emps";
        Query query = session.createQuery(hql);
        List<Department> result = query.list();
        System.out.println(result);
        for (Department obj : result)
        {//此时左外连接,Emp没有被初始化,打印多条sql21hql3,21min
            System.out.println(Arrays.asList(obj.getEmps()));
        }
    }
    
    @Test
    public void testQBC()
    {
        //1.创建一个Criteria对象
        Criteria criteria = session.createCriteria(Employee.class);
        //2.添加查询条件:在QBC查询中条件查询使用Critrion表示
        //Criterion可以通过Restrictions的静态方法得到
        criteria.add(Restrictions.gt("salary", 1000F));

        //3.执行查询
        Employee emp = (Employee) criteria.uniqueResult();
        System.out.println(emp);
    }

    @Test
    public void testQBC2()
    {
        Criteria criteria = session.createCriteria(Employee.class);
        //1.AND:使用Conjunction表示
        //Conjunction本身就是一个Criterion对象
        //
        Conjunction conjunction = Restrictions.conjunction();
        conjunction.add(Restrictions.like("name", "a", MatchMode.ANYWHERE));
        Department dept = new Department();
        dept.setId(1);
        conjunction.add(Restrictions.eqOrIsNull("dept", dept));
        System.out.println(conjunction);

        //or:使用Disjunction表示
        Disjunction disjunction = Restrictions.disjunction();
        disjunction.add(Restrictions.ge("salary", 6000F));
        disjunction.add(Restrictions.isNull("email"));
        criteria.add(disjunction);
        System.out.println(disjunction);
        //执行查询
        Employee emp = (Employee) criteria.uniqueResult();
    }

    @Test
    public void testQBC3()
    {
        Criteria criteria = session.createCriteria(Employee.class);
        //统计查询:使用Projection来表示:可以由Projections的静态方法得到
        criteria.setProjection(Projections.max("salary"));
        System.out.println(criteria.uniqueResult());
    }

    @Test
    public void testQBC4()
    {
        //QBC排序,分页
        Criteria criteria = session.createCriteria(Employee.class);
        //1.添加排序
        criteria.addOrder(Order.asc("salary"));
        criteria.addOrder(Order.desc("email"));
        //添加分页方法
        int pageSize = 5;
        int pageNo = 3;
        criteria.setFirstResult((pageNo - 1) * pageSize).setMaxResults(pageSize);
        System.out.println(criteria.uniqueResult());
    }

    //hibernate的二级缓存
    @Test
    public void testHibernateSecondLevelCache()
    {
        Employee employee = (Employee) session.get(Employee.class, 100);
        System.out.println(employee.getName());

        transaction.commit();
        session.close();
        //二级缓存用来保证当新开一个session时,仍可以使用缓存
        //一级缓存,session级别缓存,由hibernate管理
        //二级缓存,SessionFactory级别的缓存:
        //	[1]内置缓存[2]外置缓存
        session = sessionFactory.openSession();
        transaction = session.beginTransaction();
        Employee employee2 = (Employee) session.get(Employee.class, 100);
        System.out.println(employee2.getName());
    }

    @Test
    public void testCollectionSecondLevelCache()
    {
        Department dept = (Department) session.get(Department.class, 1);
        System.out.println(dept.getName());
        System.out.println(dept.getEmps().size());
        transaction.commit();
        session.close();
        session = sessionFactory.openSession();
        transaction = session.beginTransaction();
        Department dept2 = (Department) session.get(Department.class, 1);
        System.out.println(dept2.getName());
        System.out.println(dept2.getEmps().size());
    }
}
