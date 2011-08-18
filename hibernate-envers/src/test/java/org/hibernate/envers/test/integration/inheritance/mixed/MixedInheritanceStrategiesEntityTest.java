package org.hibernate.envers.test.integration.inheritance.mixed;

import org.hibernate.ejb.Ejb3Configuration;
import org.hibernate.envers.test.AbstractEntityTest;
import org.hibernate.envers.test.Priority;
import org.hibernate.envers.test.integration.inheritance.mixed.entities.*;
import org.junit.Test;

import java.util.Arrays;

import static org.junit.Assert.assertEquals;

/**
 * @author Michal Skowronek (mskowr at o2 pl)
 */
public class MixedInheritanceStrategiesEntityTest extends AbstractEntityTest {

	private ActivityId id2;
	private ActivityId id1;
	private ActivityId id3;

	@Override
    public void configure(Ejb3Configuration cfg) {
        cfg.addAnnotatedClass(AbstractActivity.class);
        cfg.addAnnotatedClass(AbstractCheckActivity.class);
        cfg.addAnnotatedClass(CheckInActivity.class);
        cfg.addAnnotatedClass(NormalActivity.class);
    }

    @Test
    @Priority(10)
    public void initData() {
        NormalActivity normalActivity = new NormalActivity();
		id1 = new ActivityId(1, 2);
		normalActivity.setId(id1);
        normalActivity.setSequenceNumber(1);

        // Revision 1
        getEntityManager().getTransaction().begin();

        getEntityManager().persist(normalActivity);

        getEntityManager().getTransaction().commit();
        // Revision 2
        getEntityManager().getTransaction().begin();

        normalActivity = getEntityManager().find(NormalActivity.class, id1);
        CheckInActivity checkInActivity = new CheckInActivity();
		id2 = new ActivityId(2, 3);
		checkInActivity.setId(id2);
        checkInActivity.setSequenceNumber(0);
        checkInActivity.setDurationInMinutes(30);
        checkInActivity.setRelatedActivity(normalActivity);

        getEntityManager().persist(checkInActivity);

        getEntityManager().getTransaction().commit();

        // Revision 3
        normalActivity = new NormalActivity();
		id3 = new ActivityId(3, 4);
		normalActivity.setId(id3);
        normalActivity.setSequenceNumber(2);

        getEntityManager().getTransaction().begin();

        getEntityManager().persist(normalActivity);

        getEntityManager().getTransaction().commit();

        // Revision 4
        getEntityManager().getTransaction().begin();

        normalActivity = getEntityManager().find(NormalActivity.class, id3);
        checkInActivity = getEntityManager().find(CheckInActivity.class, id2);
        checkInActivity.setRelatedActivity(normalActivity);

        getEntityManager().merge(checkInActivity);

        getEntityManager().getTransaction().commit();
    }

    @Test
    public void testRevisionsCounts() {
        assertEquals(Arrays.asList(1), getAuditReader().getRevisions(NormalActivity.class, id1));
        assertEquals(Arrays.asList(3), getAuditReader().getRevisions(NormalActivity.class, id3));
        assertEquals(Arrays.asList(2, 4), getAuditReader().getRevisions(CheckInActivity.class, id2));
    }

    @Test
    public void testCurrentStateOfCheckInActivity() {

        final CheckInActivity checkInActivity = getEntityManager().find(CheckInActivity.class, id2);
        final NormalActivity normalActivity = getEntityManager().find(NormalActivity.class, id3);

        assertEquals(id2, checkInActivity.getId());
        assertEquals(0, checkInActivity.getSequenceNumber().intValue());
        assertEquals(30, checkInActivity.getDurationInMinutes().intValue());
        final Activity relatedActivity = checkInActivity.getRelatedActivity();
        assertEquals(normalActivity.getId(), relatedActivity.getId());
        assertEquals(normalActivity.getSequenceNumber(), relatedActivity.getSequenceNumber());
    }

    @Test
    public void testCheckCurrentStateOfNormalActivities() throws Exception {
        final NormalActivity normalActivity1 = getEntityManager().find(NormalActivity.class, id1);
        final NormalActivity normalActivity2 = getEntityManager().find(NormalActivity.class, id3);

        assertEquals(id1, normalActivity1.getId());
        assertEquals(1, normalActivity1.getSequenceNumber().intValue());
        assertEquals(id3, normalActivity2.getId());
        assertEquals(2, normalActivity2.getSequenceNumber().intValue());
    }

    @Test
    public void doTestFirstRevisionOfCheckInActivity() throws Exception {
        CheckInActivity checkInActivity = getAuditReader().find(CheckInActivity.class, id2, 2);
        NormalActivity normalActivity = getAuditReader().find(NormalActivity.class, id1, 2);

        assertEquals(id2, checkInActivity.getId());
        assertEquals(0, checkInActivity.getSequenceNumber().intValue());
        assertEquals(30, checkInActivity.getDurationInMinutes().intValue());
        Activity relatedActivity = checkInActivity.getRelatedActivity();
        assertEquals(normalActivity.getId(), relatedActivity.getId());
        assertEquals(normalActivity.getSequenceNumber(), relatedActivity.getSequenceNumber());
    }

    @Test
    public void doTestSecondRevisionOfCheckInActivity() throws Exception {
        CheckInActivity checkInActivity = getAuditReader().find(CheckInActivity.class, id2, 4);
        NormalActivity normalActivity = getAuditReader().find(NormalActivity.class, id3, 4);

        assertEquals(id2, checkInActivity.getId());
        assertEquals(0, checkInActivity.getSequenceNumber().intValue());
        assertEquals(30, checkInActivity.getDurationInMinutes().intValue());
        Activity relatedActivity = checkInActivity.getRelatedActivity();
        assertEquals(normalActivity.getId(), relatedActivity.getId());
        assertEquals(normalActivity.getSequenceNumber(), relatedActivity.getSequenceNumber());
    }

}
