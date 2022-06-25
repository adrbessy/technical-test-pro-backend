package com.maiia.pro.service;

import com.maiia.pro.entity.Appointment;
import com.maiia.pro.entity.Availability;
import com.maiia.pro.entity.TimeSlot;
import com.maiia.pro.repository.AppointmentRepository;
import com.maiia.pro.repository.AvailabilityRepository;
import com.maiia.pro.repository.TimeSlotRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
public class ProAvailabilityService {

    @Autowired
    private AvailabilityRepository availabilityRepository;

    @Autowired
    private AppointmentRepository appointmentRepository;

    @Autowired
    private TimeSlotRepository timeSlotRepository;

    public List<Availability> findByPractitionerId(Integer practitionerId) {
        return availabilityRepository.findByPractitionerId(practitionerId);
    }

    /**
     * Generates the availabilities from the timeSlots and the appointments of a particular practitioner.
     * @param practitionerId the id of the practitioner
     * @return the availabilities list
     */
    public List<Availability> generateAvailabilities(Integer practitionerId) {
        List<TimeSlot> timeSlotList = timeSlotRepository.findByPractitionerId(practitionerId);
        List<Availability> availabilityList =  availabilityRepository.findByPractitionerId(practitionerId);

        for(TimeSlot timeSlot : timeSlotList){
            Availability availability = Availability.builder().practitionerId(practitionerId)
                    .startDate(timeSlot.getStartDate()).endDate(timeSlot.getEndDate()).build();
            availabilityList.add(availability);
        }

        availabilityList.sort(Comparator.comparing(Availability::getStartDate));

        // delete duplicates and merge or concatenate availabilities that overlap
        Availability tempAvailability = null;
        List<Availability> cleanAvailabilityList = new ArrayList<>();
        int i = 0;
        for (Availability availability : availabilityList){
            if (tempAvailability == null){
                tempAvailability = availability;
                if(i++ == availabilityList.size() - 1){
                    cleanAvailabilityList.add(tempAvailability);
                }
            }
            // if duplicated or contains availability
            else if ((availability.getStartDate().isEqual(tempAvailability.getStartDate())
            && availability.getEndDate().isEqual(tempAvailability.getEndDate()))
            || (tempAvailability.getStartDate().isBefore(availability.getStartDate())
                    && tempAvailability.getEndDate().isAfter(availability.getEndDate())) ) {
                if(i++ == availabilityList.size() - 1){
                    cleanAvailabilityList.add(tempAvailability);
                }
            }
            // if tempAvailability is contained by availability
            else if ((tempAvailability.getStartDate().isAfter(availability.getStartDate())
                    || tempAvailability.getStartDate().isEqual(availability.getStartDate()))
                    && tempAvailability.getEndDate().isBefore(availability.getEndDate())) {
                tempAvailability = Availability.builder().practitionerId(practitionerId)
                        .startDate(availability.getStartDate()).endDate(availability.getEndDate()).build();
                if(i++ == availabilityList.size() - 1){
                    cleanAvailabilityList.add(tempAvailability);
                }
            }
            // if tempAvailability overlaps from the left availability
            else if (tempAvailability.getStartDate().isBefore(availability.getStartDate())
                    && (tempAvailability.getEndDate().isAfter(availability.getStartDate())
                        || tempAvailability.getEndDate().isEqual(availability.getEndDate())) ) {
                tempAvailability = Availability.builder().practitionerId(practitionerId)
                        .startDate(tempAvailability.getStartDate()).endDate(availability.getEndDate()).build();
                if(i++ == availabilityList.size() - 1){
                    cleanAvailabilityList.add(tempAvailability);
                }
            }
            // if tempAvailability overlaps from the right availability
            else if (tempAvailability.getEndDate().isAfter(availability.getEndDate())
                    && (tempAvailability.getStartDate().isBefore(availability.getEndDate())
                    || tempAvailability.getStartDate().isEqual(availability.getStartDate())) ) {
                tempAvailability = Availability.builder().practitionerId(practitionerId)
                        .startDate(availability.getStartDate()).endDate(tempAvailability.getEndDate()).build();
                if(i++ == availabilityList.size() - 1){
                    cleanAvailabilityList.add(tempAvailability);
                }
            }
            else if (tempAvailability.getEndDate().isEqual(availability.getStartDate())) {
                tempAvailability = Availability.builder().practitionerId(practitionerId)
                        .startDate(tempAvailability.getStartDate()).endDate(availability.getEndDate()).build();
                if(i++ == availabilityList.size() - 1){
                    cleanAvailabilityList.add(tempAvailability);
                }
            } else if (availability.getEndDate().isEqual(tempAvailability.getStartDate())) {
                tempAvailability = Availability.builder().practitionerId(practitionerId)
                        .startDate(availability.getStartDate()).endDate(tempAvailability.getEndDate()).build();
                if(i++ == availabilityList.size() - 1){
                    cleanAvailabilityList.add(tempAvailability);
                }
            } else{
                cleanAvailabilityList.add(tempAvailability);
                if(i++ == availabilityList.size() - 1){
                    cleanAvailabilityList.add(availability);
                } else {
                    tempAvailability = availability;
                }
            }
        }

        // Takes into account the appointments
        List<Appointment> appointmentList = appointmentRepository.findByPractitionerId(practitionerId);
        List<Availability> newAvailabilityList = new ArrayList<>();
        for(Availability availability : cleanAvailabilityList){
            List<Availability> tempAvailabilityList = new ArrayList<>();
            tempAvailabilityList.add(availability);
            for(Appointment appointment : appointmentList){
                List<Availability> toRemove = new ArrayList<>();
                List<Availability> toAdd = new ArrayList<>();
                for(Availability avail : tempAvailabilityList) {
                    // if appointment is contained by the availability with the same end date
                    if(appointment.getStartDate().isAfter(avail.getStartDate())
                            && appointment.getEndDate().isEqual(avail.getEndDate())){
                        Availability availability1 = Availability.builder().practitionerId(practitionerId)
                                .startDate(avail.getStartDate()).endDate(appointment.getStartDate()).build();
                        toRemove.add(avail);
                        toAdd.add(availability1);
                    }
                    // if appointment is contained by the availability
                    else if (appointment.getStartDate().isAfter(avail.getStartDate())
                            && appointment.getStartDate().isBefore(avail.getEndDate())) {
                        Availability availability1 = Availability.builder().practitionerId(practitionerId)
                                .startDate(avail.getStartDate()).endDate(appointment.getStartDate()).build();
                        Availability availability2 = Availability.builder().practitionerId(practitionerId)
                                .startDate(appointment.getEndDate()).endDate(avail.getEndDate()).build();
                        toRemove.add(avail);
                        toAdd.add(availability1);
                        toAdd.add(availability2);
                    }
                    // if appointment is contained by the availability with the same start date
                    else if (appointment.getStartDate().isEqual(avail.getStartDate())
                            && appointment.getStartDate().isBefore(avail.getEndDate())) {
                        Availability availability1 = Availability.builder().practitionerId(practitionerId)
                                .startDate(appointment.getEndDate()).endDate(avail.getEndDate()).build();
                        toRemove.add(avail);
                        toAdd.add(availability1);
                    }
                }
                tempAvailabilityList.removeAll(toRemove);
                tempAvailabilityList.addAll(toAdd);
            }
            for(Availability avail : tempAvailabilityList){
                newAvailabilityList.add(avail);
                availabilityRepository.save(avail);
            }
        }
        return newAvailabilityList;
    }

}
