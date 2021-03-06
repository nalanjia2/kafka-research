package com.aebiz.util;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.PartitionInfo;
import org.apache.kafka.common.TopicPartition;
import org.springframework.util.CollectionUtils;

import com.aebiz.vo.ResearchPartitionInfoDTO;
import com.aebiz.vo.ResearchTopicInfoDTO;

public class TopicUtil {

	public static ResearchTopicInfoDTO getTopicDetail(String topicName) {
		ResearchTopicInfoDTO tInfo = new ResearchTopicInfoDTO();
		tInfo.setReturnTime(DateUtil.getNowTime_EN());
		
		String groupId = ConsumerUtil.getGroupId();
		KafkaConsumer consumer = ConsumerUtil.getKafkaConsumer(groupId);
		//查询该主题的所有分区
		List<PartitionInfo> pList = consumer.partitionsFor(topicName);
		
		if(CollectionUtils.isEmpty(pList)) {
			//关闭consumer
			ConsumerGroupUtil.deleteConsumerGroup(groupId);
			return tInfo;
		}
		
		List<TopicPartition> tpList = new ArrayList<>();
		pList.forEach(p -> {
			TopicPartition tp = new TopicPartition(topicName, p.partition());
			tpList.add(tp);
		});
		//为消费者指定分区
		consumer.assign(tpList);
		
		//查询各个分区的first offset，end offset
		Map<TopicPartition, Long> beginMap = consumer.beginningOffsets(tpList);
		Map<TopicPartition, Long> endMap = consumer.endOffsets(tpList);
		
		tpList.forEach(t -> {
			Long beginOffset = beginMap.get(t);
			Long endOffset = endMap.get(t);
			
			long logSize = endOffset.longValue() - beginOffset.longValue();
			ResearchPartitionInfoDTO researchP = new ResearchPartitionInfoDTO();
			researchP.setLogSize(logSize);
			researchP.setPartition(t.partition()); //分区编号
			
			tInfo.setLogSize(tInfo.getLogSize() + logSize);
			tInfo.getPartitionList().add(researchP);
		});
		
		//关闭consumer
		ConsumerGroupUtil.deleteConsumerGroup(groupId);
		return tInfo;
	}
	
}
