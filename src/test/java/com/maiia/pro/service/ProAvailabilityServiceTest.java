package com.maiia.pro.service;

import com.maiia.pro.EntityFactory;
import com.maiia.pro.entity.Availability;
import com.maiia.pro.entity.Practitioner;
import com.maiia.pro.repository.AppointmentRepository;
import com.maiia.pro.repository.AvailabilityRepository;
import com.maiia.pro.repository.PractitionerRepository;
import com.maiia.pro.repository.TimeSlotRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDateTime;
import java.time.Month;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class ProAvailabilityServiceTest {
    private final  EntityFactory entityFactory = new EntityFactory();
    private  final static Integer patient_id=657679;
    @Autowired
    private ProAvailabilityService proAvailabilityService;

    @Autowired
    private AppointmentRepository appointmentRepository;

    @Autowired
    private AvailabilityRepository availabilityRepository;

    @Autowired
    private PractitionerRepository practitionerRepository;

    @Autowired
    private TimeSlotRepository timeSlotRepository;

    @Test
    void generateAvailabilitiesWithDistinctTimeSlots() {
        Practitioner practitioner = practitionerRepository.save(entityFactory.createPractitioner());
        LocalDateTime startDate = LocalDateTime.of(2020, Month.FEBRUARY, 5, 11, 0, 0);
        timeSlotRepository.save(entityFactory.createTimeSlot(practitioner.getId(), startDate, startDate.plusHours(1)));
        timeSlotRepository.save(entityFactory.createTimeSlot(practitioner.getId(), startDate.plusHours(2), startDate.plusHours(3)));

        List<Availability> availabilities = proAvailabilityService.generateAvailabilities(practitioner.getId());

        assertEquals(2, availabilities.size());

        List<LocalDateTime> availabilitiesStartDate = availabilities.stream().map(Availability::getStartDate).collect(Collectors.toList());
        ArrayList<LocalDateTime> expectedStartDate = new ArrayList<>();
        expectedStartDate.add(startDate);
        expectedStartDate.add(startDate.plusHours(2));
        assertTrue(availabilitiesStartDate.containsAll(expectedStartDate));

        List<LocalDateTime> availabilitiesEndDate = availabilities.stream().map(Availability::getEndDate).collect(Collectors.toList());
        ArrayList<LocalDateTime> expectedEndDate = new ArrayList<>();
        expectedEndDate.add(startDate.plusHours(1));
        expectedEndDate.add(startDate.plusHours(3));
        assertTrue(availabilitiesEndDate.containsAll(expectedEndDate));
    }

    @Test
    void generateAvailabilitiesWithConsecutiveTimeSlots() {
        Practitioner practitioner = practitionerRepository.save(entityFactory.createPractitioner());
        LocalDateTime startDate = LocalDateTime.of(2020, Month.FEBRUARY, 5, 11, 0, 0);
        timeSlotRepository.save(entityFactory.createTimeSlot(practitioner.getId(), startDate, startDate.plusHours(1)));
        timeSlotRepository.save(entityFactory.createTimeSlot(practitioner.getId(), startDate.plusHours(1), startDate.plusHours(2)));

        List<Availability> availabilities = proAvailabilityService.generateAvailabilities(practitioner.getId());

        // the availabilities had been concatenated
        assertEquals(1, availabilities.size());

        List<LocalDateTime> availabilitiesStartDate = availabilities.stream().map(Availability::getStartDate).collect(Collectors.toList());
        ArrayList<LocalDateTime> expectedStartDate = new ArrayList<>();
        expectedStartDate.add(startDate);
        assertTrue(availabilitiesStartDate.containsAll(expectedStartDate));

        List<LocalDateTime> availabilitiesEndDate = availabilities.stream().map(Availability::getEndDate).collect(Collectors.toList());
        ArrayList<LocalDateTime> expectedEndDate = new ArrayList<>();
        expectedEndDate.add(startDate.plusHours(2));
        assertTrue(availabilitiesEndDate.containsAll(expectedEndDate));
    }

    @Test
    void generateAvailabilitiesWithConsecutiveTimeslots2() {
        Practitioner practitioner = practitionerRepository.save(entityFactory.createPractitioner());
        LocalDateTime startDate = LocalDateTime.of(2020, Month.FEBRUARY, 5, 11, 0, 0);
        // the time slots are saved in another order
        timeSlotRepository.save(entityFactory.createTimeSlot(practitioner.getId(), startDate.plusHours(1), startDate.plusHours(2)));
        timeSlotRepository.save(entityFactory.createTimeSlot(practitioner.getId(), startDate, startDate.plusHours(1)));

        List<Availability> availabilities = proAvailabilityService.generateAvailabilities(practitioner.getId());

        // the availabilities had been concatenated
        assertEquals(1, availabilities.size());

        List<LocalDateTime> availabilitiesStartDate = availabilities.stream().map(Availability::getStartDate).collect(Collectors.toList());
        ArrayList<LocalDateTime> expectedStartDate = new ArrayList<>();
        expectedStartDate.add(startDate);
        assertTrue(availabilitiesStartDate.containsAll(expectedStartDate));

        List<LocalDateTime> availabilitiesEndDate = availabilities.stream().map(Availability::getEndDate).collect(Collectors.toList());
        ArrayList<LocalDateTime> expectedEndDate = new ArrayList<>();
        expectedEndDate.add(startDate.plusHours(2));
        assertTrue(availabilitiesEndDate.containsAll(expectedEndDate));
    }

    @Test
    void checkAvailabilityIsNotDuplicated() {
        Practitioner practitioner = practitionerRepository.save(entityFactory.createPractitioner());
        LocalDateTime startDate = LocalDateTime.of(2020, Month.FEBRUARY, 5, 11, 0, 0);
        // is exactly the same as an existing availability
        timeSlotRepository.save(entityFactory.createTimeSlot(practitioner.getId(), startDate, startDate.plusMinutes(15)));

        availabilityRepository.save(Availability.builder().practitionerId(practitioner.getId()).startDate(startDate).endDate(startDate.plusMinutes(15)).build());
        List<Availability> availabilities = proAvailabilityService.generateAvailabilities(practitioner.getId());

        // Not duplicated
        assertEquals(1, availabilities.size());

        List<LocalDateTime> availabilitiesStartDate = availabilities.stream().map(Availability::getStartDate).collect(Collectors.toList());
        ArrayList<LocalDateTime> expectedStartDate = new ArrayList<>();
        expectedStartDate.add(startDate);
        assertTrue(availabilitiesStartDate.containsAll(expectedStartDate));

        List<LocalDateTime> availabilitiesEndDate = availabilities.stream().map(Availability::getEndDate).collect(Collectors.toList());
        ArrayList<LocalDateTime> expectedEndDate = new ArrayList<>();
        expectedEndDate.add(startDate.plusMinutes(15));
        assertTrue(availabilitiesEndDate.containsAll(expectedEndDate));
    }

    @Test
    void checkAvailabilityIsNotDuplicated2() {
        Practitioner practitioner = practitionerRepository.save(entityFactory.createPractitioner());
        LocalDateTime startDate = LocalDateTime.of(2020, Month.FEBRUARY, 5, 11, 0, 0);
        // will contain an existing availability with the same start date
        timeSlotRepository.save(entityFactory.createTimeSlot(practitioner.getId(), startDate, startDate.plusHours(1)));

        availabilityRepository.save(Availability.builder().practitionerId(practitioner.getId()).startDate(startDate).endDate(startDate.plusMinutes(15)).build());

        List<Availability> availabilities = proAvailabilityService.generateAvailabilities(practitioner.getId());
        assertEquals(1, availabilities.size());

        List<LocalDateTime> availabilitiesStartDate = availabilities.stream().map(Availability::getStartDate).collect(Collectors.toList());
        ArrayList<LocalDateTime> expectedStartDate = new ArrayList<>();
        expectedStartDate.add(startDate);
        assertTrue(availabilitiesStartDate.containsAll(expectedStartDate));

        List<LocalDateTime> availabilitiesEndDate = availabilities.stream().map(Availability::getEndDate).collect(Collectors.toList());
        ArrayList<LocalDateTime> expectedEndDate = new ArrayList<>();
        expectedEndDate.add(startDate.plusHours(1));
        assertTrue(availabilitiesEndDate.containsAll(expectedEndDate));
    }

    @Test
    void checkAvailabilityIsNotDuplicated3() {
        Practitioner practitioner = practitionerRepository.save(entityFactory.createPractitioner());
        LocalDateTime startDate = LocalDateTime.of(2020, Month.FEBRUARY, 5, 11, 0, 0);
        // will overlap the start of an existing availability with the same end date
        timeSlotRepository.save(entityFactory.createTimeSlot(practitioner.getId(), startDate.minusMinutes(10), startDate.plusMinutes(15)));

        availabilityRepository.save(Availability.builder().practitionerId(practitioner.getId()).startDate(startDate).endDate(startDate.plusMinutes(15)).build());

        List<Availability> availabilities = proAvailabilityService.generateAvailabilities(practitioner.getId());
        assertEquals(1, availabilities.size());

        List<LocalDateTime> availabilitiesStartDate = availabilities.stream().map(Availability::getStartDate).collect(Collectors.toList());
        ArrayList<LocalDateTime> expectedStartDate = new ArrayList<>();
        expectedStartDate.add(startDate.minusMinutes(10));
        assertTrue(availabilitiesStartDate.containsAll(expectedStartDate));

        List<LocalDateTime> availabilitiesEndDate = availabilities.stream().map(Availability::getEndDate).collect(Collectors.toList());
        ArrayList<LocalDateTime> expectedEndDate = new ArrayList<>();
        expectedEndDate.add(startDate.plusMinutes(15));
        assertTrue(availabilitiesEndDate.containsAll(expectedEndDate));
    }

    @Test
    void checkAvailabilityIsNotDuplicated4() {
        Practitioner practitioner = practitionerRepository.save(entityFactory.createPractitioner());
        LocalDateTime startDate = LocalDateTime.of(2020, Month.FEBRUARY, 5, 11, 0, 0);
        // will overlap the start of an existing availability
        timeSlotRepository.save(entityFactory.createTimeSlot(practitioner.getId(), startDate.minusMinutes(10), startDate.plusMinutes(10)));

        availabilityRepository.save(Availability.builder().practitionerId(practitioner.getId()).startDate(startDate).endDate(startDate.plusMinutes(15)).build());

        List<Availability> availabilities = proAvailabilityService.generateAvailabilities(practitioner.getId());
        assertEquals(1, availabilities.size());

        List<LocalDateTime> availabilitiesStartDate = availabilities.stream().map(Availability::getStartDate).collect(Collectors.toList());
        ArrayList<LocalDateTime> expectedStartDate = new ArrayList<>();
        expectedStartDate.add(startDate.minusMinutes(10));
        assertTrue(availabilitiesStartDate.containsAll(expectedStartDate));

        List<LocalDateTime> availabilitiesEndDate = availabilities.stream().map(Availability::getEndDate).collect(Collectors.toList());
        ArrayList<LocalDateTime> expectedEndDate = new ArrayList<>();
        expectedEndDate.add(startDate.plusMinutes(15));
        assertTrue(availabilitiesEndDate.containsAll(expectedEndDate));
    }

    @Test
    void checkAvailabilityIsNotDuplicated5() {
        Practitioner practitioner = practitionerRepository.save(entityFactory.createPractitioner());
        LocalDateTime startDate = LocalDateTime.of(2020, Month.FEBRUARY, 5, 11, 0, 0);
        // will be contained by an existing availability
        timeSlotRepository.save(entityFactory.createTimeSlot(practitioner.getId(), startDate.plusMinutes(5), startDate.plusMinutes(10)));

        availabilityRepository.save(Availability.builder().practitionerId(practitioner.getId()).startDate(startDate).endDate(startDate.plusMinutes(15)).build());

        List<Availability> availabilities = proAvailabilityService.generateAvailabilities(practitioner.getId());
        assertEquals(1, availabilities.size());

        List<LocalDateTime> availabilitiesStartDate = availabilities.stream().map(Availability::getStartDate).collect(Collectors.toList());
        ArrayList<LocalDateTime> expectedStartDate = new ArrayList<>();
        expectedStartDate.add(startDate);
        assertTrue(availabilitiesStartDate.containsAll(expectedStartDate));

        List<LocalDateTime> availabilitiesEndDate = availabilities.stream().map(Availability::getEndDate).collect(Collectors.toList());
        ArrayList<LocalDateTime> expectedEndDate = new ArrayList<>();
        expectedEndDate.add(startDate.plusMinutes(15));
        assertTrue(availabilitiesEndDate.containsAll(expectedEndDate));
    }

    @Test
    void checkAvailabilityIsNotDuplicated6() {
        Practitioner practitioner = practitionerRepository.save(entityFactory.createPractitioner());
        LocalDateTime startDate = LocalDateTime.of(2020, Month.FEBRUARY, 5, 11, 0, 0);
        // will overlap the end of an existing availability
        timeSlotRepository.save(entityFactory.createTimeSlot(practitioner.getId(), startDate.plusMinutes(10), startDate.plusMinutes(20)));

        availabilityRepository.save(Availability.builder().practitionerId(practitioner.getId()).startDate(startDate).endDate(startDate.plusMinutes(15)).build());

        List<Availability> availabilities = proAvailabilityService.generateAvailabilities(practitioner.getId());
        assertEquals(1, availabilities.size());

        List<LocalDateTime> availabilitiesStartDate = availabilities.stream().map(Availability::getStartDate).collect(Collectors.toList());
        ArrayList<LocalDateTime> expectedStartDate = new ArrayList<>();
        expectedStartDate.add(startDate);
        assertTrue(availabilitiesStartDate.containsAll(expectedStartDate));

        List<LocalDateTime> availabilitiesEndDate = availabilities.stream().map(Availability::getEndDate).collect(Collectors.toList());
        ArrayList<LocalDateTime> expectedEndDate = new ArrayList<>();
        expectedEndDate.add(startDate.plusMinutes(20));
        assertTrue(availabilitiesEndDate.containsAll(expectedEndDate));
    }

    @Test
    void checkAvailabilityIsNotDuplicated7() {
        Practitioner practitioner = practitionerRepository.save(entityFactory.createPractitioner());
        LocalDateTime startDate = LocalDateTime.of(2020, Month.FEBRUARY, 5, 11, 0, 0);
        // will overlap an existing availability with the same startDate
        timeSlotRepository.save(entityFactory.createTimeSlot(practitioner.getId(), startDate, startDate.plusMinutes(20)));

        availabilityRepository.save(Availability.builder().practitionerId(practitioner.getId()).startDate(startDate).endDate(startDate.plusMinutes(15)).build());

        List<Availability> availabilities = proAvailabilityService.generateAvailabilities(practitioner.getId());
        assertEquals(1, availabilities.size());

        List<LocalDateTime> availabilitiesStartDate = availabilities.stream().map(Availability::getStartDate).collect(Collectors.toList());
        ArrayList<LocalDateTime> expectedStartDate = new ArrayList<>();
        expectedStartDate.add(startDate);
        assertTrue(availabilitiesStartDate.containsAll(expectedStartDate));

        List<LocalDateTime> availabilitiesEndDate = availabilities.stream().map(Availability::getEndDate).collect(Collectors.toList());
        ArrayList<LocalDateTime> expectedEndDate = new ArrayList<>();
        expectedEndDate.add(startDate.plusMinutes(20));
        assertTrue(availabilitiesEndDate.containsAll(expectedEndDate));
    }

    @Test
    void generateAvailabilityWithOneAppointment() {
        Practitioner practitioner = practitionerRepository.save(entityFactory.createPractitioner());
        LocalDateTime startDate = LocalDateTime.of(2020, Month.FEBRUARY, 5, 11, 0, 0);
        timeSlotRepository.save(entityFactory.createTimeSlot(practitioner.getId(), startDate, startDate.plusHours(1)));
        // this appointment is contained in a time slot
        appointmentRepository.save(entityFactory.createAppointment(practitioner.getId(),
                patient_id,
                startDate.plusMinutes(30),
                startDate.plusMinutes(45)));

        List<Availability> availabilities = proAvailabilityService.generateAvailabilities(practitioner.getId());

        assertEquals(2, availabilities.size());

        List<LocalDateTime> availabilitiesStartDate = availabilities.stream().map(Availability::getStartDate).collect(Collectors.toList());
        ArrayList<LocalDateTime> expectedStartDate = new ArrayList<>();
        expectedStartDate.add(startDate);
        expectedStartDate.add(startDate.plusMinutes(45));
        assertTrue(availabilitiesStartDate.containsAll(expectedStartDate));

        List<LocalDateTime> availabilitiesEndDate = availabilities.stream().map(Availability::getEndDate).collect(Collectors.toList());
        ArrayList<LocalDateTime> expectedEndDate = new ArrayList<>();
        expectedEndDate.add(startDate.plusMinutes(30));
        expectedEndDate.add(startDate.plusHours(1));
        assertTrue(availabilitiesEndDate.containsAll(expectedEndDate));
    }

    @Test
    void generateAvailabilityWithOneAppointment2() {
        Practitioner practitioner = practitionerRepository.save(entityFactory.createPractitioner());
        LocalDateTime startDate = LocalDateTime.of(2020, Month.FEBRUARY, 5, 11, 0, 0);
        timeSlotRepository.save(entityFactory.createTimeSlot(practitioner.getId(), startDate, startDate.plusHours(1)));
        // this appointment is contained in a time slot with the same start date
        appointmentRepository.save(entityFactory.createAppointment(practitioner.getId(),
                patient_id,
                startDate,
                startDate.plusMinutes(15)));

        List<Availability> availabilities = proAvailabilityService.generateAvailabilities(practitioner.getId());

        assertEquals(1, availabilities.size());

        List<LocalDateTime> availabilitiesStartDate = availabilities.stream().map(Availability::getStartDate).collect(Collectors.toList());
        ArrayList<LocalDateTime> expectedStartDate = new ArrayList<>();
        expectedStartDate.add(startDate.plusMinutes(15));
        assertTrue(availabilitiesStartDate.containsAll(expectedStartDate));

        List<LocalDateTime> availabilitiesEndDate = availabilities.stream().map(Availability::getEndDate).collect(Collectors.toList());
        ArrayList<LocalDateTime> expectedEndDate = new ArrayList<>();
        expectedEndDate.add(startDate.plusHours(1));
        assertTrue(availabilitiesEndDate.containsAll(expectedEndDate));
    }

    @Test
    void generateAvailabilityWithOneAppointment3() {
        Practitioner practitioner = practitionerRepository.save(entityFactory.createPractitioner());
        LocalDateTime startDate = LocalDateTime.of(2020, Month.FEBRUARY, 5, 11, 0, 0);
        timeSlotRepository.save(entityFactory.createTimeSlot(practitioner.getId(), startDate, startDate.plusHours(1)));
        // this appointment is contained in a time slot with the same end date
        appointmentRepository.save(entityFactory.createAppointment(practitioner.getId(),
                patient_id,
                startDate.plusMinutes(15),
                startDate.plusHours(1)));

        List<Availability> availabilities = proAvailabilityService.generateAvailabilities(practitioner.getId());

        assertEquals(1, availabilities.size());

        List<LocalDateTime> availabilitiesStartDate = availabilities.stream().map(Availability::getStartDate).collect(Collectors.toList());
        ArrayList<LocalDateTime> expectedStartDate = new ArrayList<>();
        expectedStartDate.add(startDate);
        assertTrue(availabilitiesStartDate.containsAll(expectedStartDate));

        List<LocalDateTime> availabilitiesEndDate = availabilities.stream().map(Availability::getEndDate).collect(Collectors.toList());
        ArrayList<LocalDateTime> expectedEndDate = new ArrayList<>();
        expectedEndDate.add(startDate.plusMinutes(15));
        assertTrue(availabilitiesEndDate.containsAll(expectedEndDate));
    }

    @Test
    void generateAvailabilityWithTwoAppointments() {
        Practitioner practitioner = practitionerRepository.save(entityFactory.createPractitioner());
        LocalDateTime startDate = LocalDateTime.of(2020, Month.FEBRUARY, 5, 11, 0, 0);
        timeSlotRepository.save(entityFactory.createTimeSlot(practitioner.getId(), startDate, startDate.plusHours(1)));
        appointmentRepository.save(entityFactory.createAppointment(practitioner.getId(),
                patient_id,
                startDate,
                startDate.plusMinutes(15)));

        appointmentRepository.save(entityFactory.createAppointment(practitioner.getId(),
                patient_id,
                startDate.plusMinutes(30),
                startDate.plusMinutes(45)));

        List<Availability> availabilities = proAvailabilityService.generateAvailabilities(practitioner.getId());

        assertEquals(2, availabilities.size());

        List<LocalDateTime> availabilitiesStartDate = availabilities.stream().map(Availability::getStartDate).collect(Collectors.toList());
        ArrayList<LocalDateTime> expectedStartDate = new ArrayList<>();
        expectedStartDate.add(startDate.plusMinutes(15));
        expectedStartDate.add(startDate.plusMinutes(45));
        assertTrue(availabilitiesStartDate.containsAll(expectedStartDate));

        List<LocalDateTime> availabilitiesEndDate = availabilities.stream().map(Availability::getEndDate).collect(Collectors.toList());
        ArrayList<LocalDateTime> expectedEndDate = new ArrayList<>();
        expectedEndDate.add(startDate.plusMinutes(30));
        expectedEndDate.add(startDate.plusHours(1));
        assertTrue(availabilitiesEndDate.containsAll(expectedEndDate));
    }

    @Test
    void generateAvailabilityWithTwoAppointments2() {
        Practitioner practitioner = practitionerRepository.save(entityFactory.createPractitioner());
        LocalDateTime startDate = LocalDateTime.of(2020, Month.FEBRUARY, 5, 11, 0, 0);
        timeSlotRepository.save(entityFactory.createTimeSlot(practitioner.getId(), startDate, startDate.plusHours(1)));
        appointmentRepository.save(entityFactory.createAppointment(practitioner.getId(),
                patient_id,
                startDate.plusMinutes(15),
                startDate.plusMinutes(30)));

        appointmentRepository.save(entityFactory.createAppointment(practitioner.getId(),
                patient_id,
                startDate.plusMinutes(45),
                startDate.plusHours(1)));

        List<Availability> availabilities = proAvailabilityService.generateAvailabilities(practitioner.getId());

        assertEquals(2, availabilities.size());

        List<LocalDateTime> availabilitiesStartDate = availabilities.stream().map(Availability::getStartDate).collect(Collectors.toList());
        ArrayList<LocalDateTime> expectedStartDate = new ArrayList<>();
        expectedStartDate.add(startDate);
        expectedStartDate.add(startDate.plusMinutes(30));
        assertTrue(availabilitiesStartDate.containsAll(expectedStartDate));

        List<LocalDateTime> availabilitiesEndDate = availabilities.stream().map(Availability::getEndDate).collect(Collectors.toList());
        ArrayList<LocalDateTime> expectedEndDate = new ArrayList<>();
        expectedEndDate.add(startDate.plusMinutes(15));
        expectedEndDate.add(startDate.plusMinutes(45));
        assertTrue(availabilitiesEndDate.containsAll(expectedEndDate));
    }

    @Test
    void generateAvailabilityWithFourAppointments() {
        Practitioner practitioner = practitionerRepository.save(entityFactory.createPractitioner());
        LocalDateTime startDate = LocalDateTime.of(2020, Month.FEBRUARY, 5, 11, 0, 0);
        timeSlotRepository.save(entityFactory.createTimeSlot(practitioner.getId(), startDate, startDate.plusHours(1)));
        appointmentRepository.save(entityFactory.createAppointment(practitioner.getId(),
                patient_id,
                startDate.plusMinutes(5),
                startDate.plusMinutes(10)));

        appointmentRepository.save(entityFactory.createAppointment(practitioner.getId(),
                patient_id,
                startDate.plusMinutes(15),
                startDate.plusMinutes(25)));

        appointmentRepository.save(entityFactory.createAppointment(practitioner.getId(),
                patient_id,
                startDate.plusMinutes(30),
                startDate.plusMinutes(40)));

        appointmentRepository.save(entityFactory.createAppointment(practitioner.getId(),
                patient_id,
                startDate.plusMinutes(45),
                startDate.plusHours(1)));

        List<Availability> availabilities = proAvailabilityService.generateAvailabilities(practitioner.getId());

        assertEquals(4, availabilities.size());

        List<LocalDateTime> availabilitiesStartDate = availabilities.stream().map(Availability::getStartDate).collect(Collectors.toList());
        ArrayList<LocalDateTime> expectedStartDate = new ArrayList<>();
        expectedStartDate.add(startDate);
        expectedStartDate.add(startDate.plusMinutes(10));
        expectedStartDate.add(startDate.plusMinutes(25));
        expectedStartDate.add(startDate.plusMinutes(40));
        assertTrue(availabilitiesStartDate.containsAll(expectedStartDate));

        List<LocalDateTime> availabilitiesEndDate = availabilities.stream().map(Availability::getEndDate).collect(Collectors.toList());
        ArrayList<LocalDateTime> expectedEndDate = new ArrayList<>();
        expectedEndDate.add(startDate.plusMinutes(5));
        expectedEndDate.add(startDate.plusMinutes(15));
        expectedEndDate.add(startDate.plusMinutes(30));
        expectedEndDate.add(startDate.plusMinutes(45));
        assertTrue(availabilitiesEndDate.containsAll(expectedEndDate));
    }

    @Test
    void generateAvailabilitiesWithAppointmentOnTwoAvailabilities() {
        Practitioner practitioner = practitionerRepository.save(entityFactory.createPractitioner());
        LocalDateTime startDate = LocalDateTime.of(2020, Month.FEBRUARY, 5, 11, 0, 0);
        timeSlotRepository.save(entityFactory.createTimeSlot(practitioner.getId(), startDate, startDate.plusHours(1)));
        timeSlotRepository.save(entityFactory.createTimeSlot(practitioner.getId(), startDate.plusHours(2), startDate.plusHours(3)));
        appointmentRepository.save(entityFactory.createAppointment(practitioner.getId(),
                patient_id,
                startDate.plusMinutes(10),
                startDate.plusMinutes(50)));
        appointmentRepository.save(entityFactory.createAppointment(practitioner.getId(),
                patient_id,
                startDate.plusMinutes(120),
                startDate.plusMinutes(160)));

        List<Availability> availabilities = proAvailabilityService.generateAvailabilities(practitioner.getId());

        assertEquals(3, availabilities.size());

        List<LocalDateTime> availabilitiesStartDate = availabilities.stream().map(Availability::getStartDate).collect(Collectors.toList());
        ArrayList<LocalDateTime> expectedStartDate = new ArrayList<>();
        expectedStartDate.add(startDate);
        expectedStartDate.add(startDate.plusMinutes(50));
        expectedStartDate.add(startDate.plusMinutes(160));
        assertTrue(availabilitiesStartDate.containsAll(expectedStartDate));

        List<LocalDateTime> availabilitiesEndDate = availabilities.stream().map(Availability::getEndDate).collect(Collectors.toList());
        ArrayList<LocalDateTime> expectedEndDate = new ArrayList<>();
        expectedEndDate.add(startDate.plusMinutes(10));
        expectedEndDate.add(startDate.plusMinutes(60));
        expectedEndDate.add(startDate.plusHours(3));
        assertTrue(availabilitiesEndDate.containsAll(expectedEndDate));
    }

}
