package persistence;

import br.ufrn.raszz.persistence.SzzDAO;
import br.ufrn.raszz.persistence.SzzDAOImpl;

public class testDAO {
    public static void main(String[] args){
        SzzDAO d = new SzzDAOImpl();
        System.out.println(d.getBugIntroducingChangeFromRASZZ("activemq"));
        System.out.println("end");
    }
}
