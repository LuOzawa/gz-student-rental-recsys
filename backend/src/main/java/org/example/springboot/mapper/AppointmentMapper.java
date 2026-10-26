package org.example.springboot.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.example.springboot.entity.Appointment;

@Mapper
public interface AppointmentMapper extends BaseMapper<Appointment> {
}
