package cn.no7player.mapper;

import cn.no7player.model.Afortune;
import org.apache.ibatis.annotations.Param;

public interface AfortuneMapper {
    /**
     * This method was generated by MyBatis Generator.
     * This method corresponds to the database table ifortune
     *
     * @mbggenerated Sun Apr 07 15:10:20 CST 2019
     */
    int deleteByPrimaryKey(Integer id);

    /**
     * This method was generated by MyBatis Generator.
     * This method corresponds to the database table ifortune
     *
     * @mbggenerated Sun Apr 07 15:10:20 CST 2019
     */
    int insert(Afortune record);

    /**
     * This method was generated by MyBatis Generator.
     * This method corresponds to the database table ifortune
     *
     * @mbggenerated Sun Apr 07 15:10:20 CST 2019
     */
    int insertSelective(Afortune record);

    /**
     * This method was generated by MyBatis Generator.
     * This method corresponds to the database table ifortune
     *
     * @mbggenerated Sun Apr 07 15:10:20 CST 2019
     */
    Afortune selectByPrimaryKey(Integer id);

    /**
     * This method was generated by MyBatis Generator.
     * This method corresponds to the database table ifortune
     *
     * @mbggenerated Sun Apr 07 15:10:20 CST 2019
     */
    int updateByPrimaryKeySelective(Afortune record);

    /**
     * This method was generated by MyBatis Generator.
     * This method corresponds to the database table ifortune
     *
     * @mbggenerated Sun Apr 07 15:10:20 CST 2019
     */
    int updateByPrimaryKey(Afortune record);

    Afortune find(@Param("name") String name, @Param("birth") String birth);
}