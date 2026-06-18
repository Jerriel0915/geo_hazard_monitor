package com.zwei.iot.alarm.dispatch;

import com.zwei.iot.alarm.dispatch.domain.AlarmDispatchRule;
import com.zwei.iot.alarm.dispatch.mapper.AlarmDispatchRuleMapper;
import com.zwei.iot.alarm.dispatch.service.impl.AlarmRuleMatcherImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AlarmRuleMatcherImplTest {

    @Mock private AlarmDispatchRuleMapper ruleMapper;
    @InjectMocks private AlarmRuleMatcherImpl matcher;

    @Test
    void matchAlarmRules_should_return_rules_with_matching_level_and_hp() {
        AlarmDispatchRule r1 = new AlarmDispatchRule();
        r1.setId(1L);
        when(ruleMapper.matchAlarmRules("2", "2"))
            .thenReturn(List.of(r1));

        List<AlarmDispatchRule> result = matcher.matchAlarmRules(2L, "2");
        assertThat(result).hasSize(1);
    }

    @Test
    void matchAlarmRules_empty_when_no_match() {
        when(ruleMapper.matchAlarmRules(anyString(), anyString()))
            .thenReturn(Collections.emptyList());

        List<AlarmDispatchRule> result = matcher.matchAlarmRules(999L, "4");
        assertThat(result).isEmpty();
    }

    @Test
    void matchOfflineRules_should_return_rules_for_device() {
        AlarmDispatchRule r = new AlarmDispatchRule();
        r.setId(10L);
        when(ruleMapper.matchOfflineRules("10"))
            .thenReturn(List.of(r));

        List<AlarmDispatchRule> result = matcher.matchOfflineRules(10L);
        assertThat(result).hasSize(1);
    }
}
