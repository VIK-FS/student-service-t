package ait.cohort5860.student.service;

import ait.cohort5860.StudentServiceApplication;
import ait.cohort5860.configuration.ServiceConfiguration;
import ait.cohort5860.student.dao.StudentRepository;
import ait.cohort5860.student.dto.ScoreDto;
import ait.cohort5860.student.dto.StudentCredentialsDto;
import ait.cohort5860.student.dto.StudentDto;
import ait.cohort5860.student.dto.StudentUpdateDto;
import ait.cohort5860.student.dto.exceptions.NotFoundException;
import ait.cohort5860.student.model.Student;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.data.mongo.MongoDataAutoConfiguration;
import org.springframework.boot.autoconfigure.mongo.MongoAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

// AAA - Arrange, Act, Assert
//@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
//@EnableAutoConfiguration(exclude = {
//        MongoAutoConfiguration.class,
//        MongoDataAutoConfiguration.class
//})


@ContextConfiguration(classes = ServiceConfiguration.class)
@SpringBootTest
public class StudentServiceTest {
    private final long studentId = 1000L;
    private String name = "John";
    private String password = "1234";
    private Student student;

    @Autowired
    private ModelMapper modelMapper;

    @MockitoBean
    private StudentRepository studentRepository;

    private StudentService studentService;

    @BeforeEach
    public void setUp() {
        student = new Student(studentId, name, password);
        studentService = new StudentServiceImpl(studentRepository, modelMapper);
    }

    @Test
    void testAddStudentWhenStudentDoesNotExist() {
        // Arrange
        StudentCredentialsDto dto = new StudentCredentialsDto(studentId, name, password);
        when(studentRepository.save(any(Student.class))).thenReturn(student);

        // Act
        boolean result = studentService.addStudent(dto);

        // Assert
        assertTrue(result);
    }

    @Test
    void testAddStudentWhenStudentExists() {
        // Arrange
        StudentCredentialsDto dto = new StudentCredentialsDto(studentId, name, password);
        when(studentRepository.existsById(dto.getId())).thenReturn(true);

        // Act
        boolean result = studentService.addStudent(dto);

        // Assert
        assertFalse(result);
        verify(studentRepository, never()).save(any(Student.class));

    }

    @Test
    void testFindStudentWhenStudentExists() {
        // Arrange
        when(studentRepository.findById(studentId)).thenReturn(Optional.ofNullable(student));

        // Act
        StudentDto studentDto = studentService.findStudent(studentId);

        // Assert
        assertNotNull(studentDto);
        assertEquals(studentId, studentDto.getId());
    }

    @Test
    void testFindStudentWhenStudentNotExists() {
        // Arrange
        when(studentRepository.findById(studentId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(NotFoundException.class, () -> studentService.findStudent(studentId));
    }

    @Test
    void testRemoveStudent() {
        // Arrange
        when(studentRepository.findById(studentId)).thenReturn(Optional.ofNullable(student));
        // Act
        StudentDto studentDto = studentService.removeStudent(studentId);

        // Assert
        assertNotNull(studentDto);
        assertEquals(studentId, studentDto.getId());
        verify(studentRepository, times(1)).deleteById(studentId);
    }

    @Test
    void testUpdateStudent() {
        // Arrange
        String newName = "NewName";
        when(studentRepository.findById(studentId)).thenReturn(Optional.ofNullable(student));
        StudentUpdateDto dto = new StudentUpdateDto(newName, null);

        // Act
        StudentCredentialsDto studentCredentialsDto = studentService.updateStudent(studentId, dto);

        // Assert
        assertNotNull(studentCredentialsDto);
        assertEquals(studentId, studentCredentialsDto.getId());
        assertEquals(newName, studentCredentialsDto.getName());
        assertEquals(password, studentCredentialsDto.getPassword());
        verify(studentRepository, times(1)).save(any(Student.class));
    }
    @Test
    void testAddScoreWhenStudentExists() {
        // Arrange
        String examName = "Math";
        Integer score = 95;
        ScoreDto scoreDto = new ScoreDto();
        // Use reflection or create object through constructor if available
        scoreDto = createScoreDto(examName, score);

        when(studentRepository.findById(studentId)).thenReturn(Optional.of(student));
        when(studentRepository.save(any(Student.class))).thenReturn(student);

        // Act
        Boolean result = studentService.addScore(studentId, scoreDto);

        // Assert
        assertTrue(result); // New exam should be added
        verify(studentRepository, times(1)).save(student);
    }

    @Test
    void testAddScoreWhenStudentNotExists() {
        // Arrange
        String examName = "Math";
        Integer score = 95;
        ScoreDto scoreDto = createScoreDto(examName, score);

        when(studentRepository.findById(studentId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(NotFoundException.class, () -> studentService.addScore(studentId, scoreDto));
        verify(studentRepository, never()).save(any(Student.class));
    }

    @Test
    void testAddScoreWhenExamAlreadyExists() {
        // Arrange
        String examName = "Math";
        Integer initialScore = 85;
        Integer newScore = 95;

        // Add initial score
        student.addScore(examName, initialScore);

        ScoreDto scoreDto = createScoreDto(examName, newScore);

        when(studentRepository.findById(studentId)).thenReturn(Optional.of(student));
        when(studentRepository.save(any(Student.class))).thenReturn(student);

        // Act
        Boolean result = studentService.addScore(studentId, scoreDto);

        // Assert
        assertFalse(result); // Exam already exists, so returns false
        verify(studentRepository, times(1)).save(student);
    }

    @Test
    void testFindStudentsByName() {
        // Arrange
        Student student1 = new Student(1L, name, "pass1");
        Student student2 = new Student(2L, name, "pass2");

        when(studentRepository.findByNameIgnoreCase(name))
                .thenReturn(Stream.of(student1, student2));

        // Act
        List<StudentDto> result = studentService.findStudentsByName(name);

        // Assert
        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals(1L, result.get(0).getId());
        assertEquals(2L, result.get(1).getId());
        assertEquals(name, result.get(0).getName());
        assertEquals(name, result.get(1).getName());
    }

    @Test
    void testFindStudentsByNameWhenNoStudentsFound() {
        // Arrange
        String nonExistentName = "NonExistent";
        when(studentRepository.findByNameIgnoreCase(nonExistentName))
                .thenReturn(Stream.empty());

        // Act
        List<StudentDto> result = studentService.findStudentsByName(nonExistentName);

        // Assert
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void testCountStudentsByNames() {
        // Arrange
        Set<String> names = Set.of("John", "Jane", "Bob");
        Long expectedCount = 5L;

        when(studentRepository.countByNameInIgnoreCase(names)).thenReturn(expectedCount);

        // Act
        Long result = studentService.countStudentsByNames(names);

        // Assert
        assertEquals(expectedCount, result);
        verify(studentRepository, times(1)).countByNameInIgnoreCase(names);
    }

    @Test
    void testFindStudentsByExamNameMinScore() {
        // Arrange
        String examName = "Math";
        Integer minScore = 90;

        Student student1 = new Student(1L, "Alice", "pass1");
        student1.addScore(examName, 95);

        Student student2 = new Student(2L, "Bob", "pass2");
        student2.addScore(examName, 92);

        when(studentRepository.findByExamNameAndScoreGreaterThanOrEqual(examName, minScore))
                .thenReturn(Stream.of(student1, student2));

        // Act
        List<StudentDto> result = studentService.findStudentsByExamNameMinScore(examName, minScore);

        // Assert
        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals(1L, result.get(0).getId());
        assertEquals(2L, result.get(1).getId());
        assertEquals("Alice", result.get(0).getName());
        assertEquals("Bob", result.get(1).getName());
    }

    @Test
    void testFindStudentsByExamNameMinScoreWhenNoStudentsFound() {
        // Arrange
        String examName = "Physics";
        Integer minScore = 100;

        when(studentRepository.findByExamNameAndScoreGreaterThanOrEqual(examName, minScore))
                .thenReturn(Stream.empty());

        // Act
        List<StudentDto> result = studentService.findStudentsByExamNameMinScore(examName, minScore);

        // Assert
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    private ScoreDto createScoreDto(String examName, Integer score) {
        try {
            ScoreDto scoreDto = new ScoreDto();
            // Use reflection to set values since ScoreDto doesn't have setters
            java.lang.reflect.Field examNameField = ScoreDto.class.getDeclaredField("examName");
            examNameField.setAccessible(true);
            examNameField.set(scoreDto, examName);

            java.lang.reflect.Field scoreField = ScoreDto.class.getDeclaredField("score");
            scoreField.setAccessible(true);
            scoreField.set(scoreDto, score);

            return scoreDto;
        } catch (Exception e) {
            throw new RuntimeException("Failed to create ScoreDto", e);
        }
    }

}
