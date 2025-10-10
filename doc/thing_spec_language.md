# 物模型定义
物模型是物理空间中的实体（如传感器、车载装置、楼宇、工厂等）在云端的数字化表示，从属性、服务和事件三个维度，分别描述了该实体是什么、能做什么、可以对外提供哪些信息。定义了物模型的这三个维度，即完成了产品功能的定义。

| 功能类型 | 说明 |
| --- | --- |
| 属性（Property） | 用于描述设备运行时具体信息和状态。例如，环境监测设备所读取的当前环境温度、智能灯开关状态、电风扇风力等级等。 属性可分为读写和只读两种类型。读写类型支持读取和设置属性值，只读类型仅支持读取属性值。|
| 服务（Service） | 指设备可供外部调用的指令或方法。服务调用中可设置输入和输出参数。输入参数是服务执行时的参数，输出参数是服务执行后的结果。相比于属性，服务可通过一条指令实现更复杂的业务逻辑，例如执行某项特定的任务。服务分为异步和同步两种调用方式。 |
| 事件（Event） | 设备运行时，主动上报给云端的信息，一般包含需要被外部感知和处理的信息、告警和故障。事件中可包含多个输出参数。例如，某项任务完成后的通知信息；设备发生故障时的温度、时间信息；设备告警时的运行状态等。事件可以被订阅和推送。|

物联网平台支持为产品定义多组功能（属性、服务和事件）。一组功能定义的集合，就是一个物模型模块。多个物模型模块，彼此互不影响

# 结构定义
```
{
    "schema": "物模型结构定义的访问URL。",
    "profile": {
        "productKey": "当前产品的ProductKey。"
    },
    "properties": [
        {
            "identifier": "属性唯一标识符（物模型模块下唯一）。",
            "name": "属性名称",
            "accessMode": "属性读写类型：只读（r）或读写（rw）。",
            "required": "是否是标准功能的必选属性：是（true），否（false）。",
            "dataType": {
                "type": "属性类型: int（原生）、float（原生）、double（原生）、text（原生）、date（String类型UTC毫秒）、bool（0或1的int类型）、enum（int类型，枚举项定义方法与bool类型定义0和1的值方法相同）、struct（结构体类型，可包含前面7种类型，下面使用"specs":[{}]描述包含的对象）、array（数组类型，支持int、double、float、text、struct）。",
                "specs": {
                    "min": "参数最小值（int、float、double类型特有）。",
                    "max": "参数最大值（int、float、double类型特有）。",
                    "unit": "属性单位（int、float、double类型特有，非必填）。",
                    "unitName": "单位名称（int、float、double类型特有，非必填）。",
                    "size": "数组元素的个数，最大512（array类型特有）。",
                    "step": "步长（text、enum类型无此参数）。",
                    "length": "数据长度，最大10240（text类型特有）。",
                    "0": "0的值（bool类型特有）。",
                    "1": "1的值（bool类型特有）。",
                    "item": {
                        "type": "数组元素的类型（array类型特有）。"
                    }
                }
            }
        }
    ],
    "events": [
        {
            "identifier": "事件唯一标识符（物模型模块下唯一，其中post是默认生成的属性上报事件）。",
            "name": "事件名称。",
            "desc": "事件描述。",
            "type": "事件类型（info、alert、error）。",
            "required": "是否是标准功能的必选事件：是（true），否（false）。",
            "outputData": [
                {
                    "identifier": "参数唯一标识符。",
                    "name": "参数名称。",
                    "dataType": {
                        "type": "属性类型: int（原生）、float（原生）、double（原生）、text（原生）、date（String类型UTC毫秒）、bool（0或1的int类型）、enum（int类型，枚举项定义方法与bool类型定义0和1的值方法相同）、struct（结构体类型，可包含前面7种类型，下面使用"specs":[{}]描述包含的对象）、array（数组类型，支持int、double、float、text、struct）。",
                        "specs": {
                            "min": "参数最小值（int、float、double类型特有）。",
                            "max": "参数最大值（int、float、double类型特有）。",
                            "unit": "属性单位（int、float、double类型特有，非必填）。",
                            "unitName": "单位名称（int、float、double类型特有，非必填）。",
                            "size": "数组元素的个数，最大512（array类型特有）。",
                            "step": "步长（text、enum类型无此参数）。",
                            "length": "数据长度，最大10240（text类型特有）。",
                            "0": "0的值（bool类型特有）。",
                            "1": "1的值（bool类型特有）。",
                            "item": {
                                "type": "数组元素的类型（array类型特有）。"
                            }
                        }
                    }
                }
            ],
            "method": "事件对应的方法名称（根据identifier生成）。"
        }
    ],
    "services": [
        {
            "identifier": "服务唯一标识符（物模型模块下唯一，其中set/get是根据属性的accessMode默认生成的服务）。",
            "name": "服务名称。",
            "desc": "服务描述。",
            "required": "是否是标准功能的必选服务：是（true），否（false）。",
            "callType": "async（异步调用）或sync（同步调用）。",
            "inputData": [
                {
                    "identifier": "入参唯一标识符。",
                    "name": "入参名称。",
                    "dataType": {
                        "type": "属性类型: int（原生）、float（原生）、double（原生）、text（原生）、date（String类型UTC毫秒）、bool（0或1的int类型）、enum（int类型，枚举项定义方法与bool类型定义0和1的值方法相同）、struct（结构体类型，可包含前面7种类型，下面使用"specs":[{}]描述包含的对象）、array（数组类型，支持int、double、float、text、struct）。",
                        "specs": {
                            "min": "参数最小值（int、float、double类型特有）。",
                            "max": "参数最大值（int、float、double类型特有）。",
                            "unit": "属性单位（int、float、double类型特有，非必填）。",
                            "unitName": "单位名称（int、float、double类型特有，非必填）。",
                            "size": "数组元素的个数，最大512（array类型特有）。",
                            "step": "步长（text、enum类型无此参数）。",
                            "length": "数据长度，最大10240（text类型特有）。",
                            "0": "0的值（bool类型特有）。",
                            "1": "1的值（bool类型特有）。",
                            "item": {
                                "type": "数组元素的类型（array类型特有）。"
                            }
                        }
                    }
                }
            ],
            "outputData": [
                {
                    "identifier": "出参唯一标识符。",
                    "name": "出参名称。",
                    "dataType": {
                        "type": "属性类型: int（原生）、float（原生）、double（原生）、text（原生）、date（String类型UTC毫秒）、bool（0或1的int类型）、enum（int类型，枚举项定义方法与bool类型定义0和1的方法相同）、struct（结构体类型，可包含前面7种类型，下面使用"specs":[{}]描述包含的对象）、array（数组类型，支持int、double、float、text、struct）。",
                        "specs": {
                            "min": "参数最小值（int、float、double类型特有）。",
                            "max": "参数最大值（int、float、double类型特有）。",
                            "unit": "属性单位（int、float、double类型特有，非必填）。",
                            "unitName": "单位名称（int、float、double类型特有，非必填）。",
                            "size": "数组元素的个数，最大512（array类型特有）。",
                            "step": "步长（text、enum类型无此参数）。",
                            "length": "数据长度，最大10240（text类型特有）。",
                            "0": "0的值（bool类型特有）。",
                            "1": "1的值（bool类型特有）。",
                            "item": {
                                "type": "数组元素的类型（array类型特有）。"
                            }
                        }
                    }
                }
            ],
            "method": "服务对应的方法名称（根据identifier生成）。"
        }
    ],
    //仅自定义模块的TSL中有以下参数。
    "functionBlockId": "自定义模块的唯一标识符，模块ID。",
    "functionBlockName": "自定义模块名称。",

    //当产品下添加了自定义模块，默认模块TSL中会包含以下参数，表示已添加的自定义模块列表。
    "functionBlocks": [
    {
      "functionBlockId": "自定义模块的唯一标识符，模块ID。",
      "functionBlockName": "自定义模块名称。",
      "productKey": "产品ProductKey。"
    }
  ]
}
```