本文介绍物模型TSL文件中JSON字段及其详细说明。

**说明** 为了完整展示TSL的结构，以下示例中包含所有参数，不代表实际使用中可能出现的组合。参数后的文字为参数说明，非参数值。各参数的使用场景，请参见参数说明。

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

若产品的节点类型是网关子设备，且接入网关协议为Modbus、OPC UA或自定义时，可以查看物模型扩展配置信息。

Modbus协议产品的扩展配置可用于Modbus子设备的物联网边缘计算，请参见[物联网边缘计算文档](https://help.aliyun.com/zh/iot-edge/support/modbus-drivers#step-ukb-ouy-v1l)。

Modbus协议产品的扩展配置数据结构如下：

```
{
    "profile": {
        "productKey": "产品ProductKey。"
    },
    "properties": [
        {
            "identifier": "属性唯一标识符（产品下唯一）。",
            "operateType": "（线圈状态/离散量输入/保持寄存器/输入寄存器: coilStatus/inputStatus/holdingRegister/inputRegister）。",
            "registerAddress": "寄存器地址。",
            "originalDataType": {
                "type": "原始数据类型：int16、uint16、int32、uint32、int64、uint64、float、double、string、bool、customized data（按大端顺序返回hex data）。",
                "specs": {
                    "registerCount": "寄存器数据个数，string、customized data特有。",
                    "swap16": "交换寄存器内高低字节，把寄存器内16位数据的前后8个bit互换（byte1byte2 -> byte2byte1），除string和bool外，其他数据类型特有。",
                    "reverseRegister": "交换寄存器顺序，把原始数据32位数据的前后16个bit互换（byte1byte2byte3byte4 -> byte3byte4byte1byte2），除string和bool外，其他数据类型特有。"
                }
            },
            "scaling": "缩放因子，除string和bool外，其他数据类型特有。",
            "trigger": "数据上报方式。1代表按时上报，2代表变更上报。",
            "writeFunctionCode":"读写操作，对于不同的operateType，可选的取值不同。coilStatus：5代表读写（读0x01，写0x05），15代表读写（读0x01，写0x0F），0代表只读0x01，6代表只写0x05，15代表只写0x0F。inputStatus：0代表只读0x02。holdingRegister：6代表读写（读0x03，写0x06），16代表读写（读0x03，写0x10），0代表只读0x03，6代表只写0x06，16代表只写0x10。inputRegister：0代表只读0x04。",
            "writeOnly":"是否只写。0代表非只写，1代表只写。",
            "pollingTime":"采集间隔，单位是ms。无需传入，将使用设备配置的采集间隔。",
            "bitMask":"掩码，bool特有。取值：1、2、4、8、16、32、64、128、256、512、1024、2048、4096、8192、16384、32768，即1<<(0~15)。"
        }
    ]
}
```

标准TSL模式报文参考：

```json
{
    "id": "123",
    "version": "1.0",
    "sys":{
        "ack":0
    },
    "params": {
        "Power": {
            "value": "on",
            "time": 1524448722000
        },
        "WF": {
            "value": 23.6,
            "time": 1524448722000
        }
    },
    "method": "thing.event.property.post"
}
```